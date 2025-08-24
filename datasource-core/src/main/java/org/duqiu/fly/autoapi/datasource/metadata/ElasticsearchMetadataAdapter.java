package org.duqiu.fly.autoapi.datasource.metadata;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch元数据适配器
 */
public class ElasticsearchMetadataAdapter implements MetadataAdapter {
    
    @Override
    public List<String> getDatabases(DataSourceConnection connection) {
        // Elasticsearch没有数据库概念，返回集群信息
        try {
            DataSourceConnection.QueryResult result = connection.executeQuery("GET /_cluster/health", Map.of());
            if (result.isSuccess() && !result.getData().isEmpty()) {
                Map<String, Object> clusterInfo = result.getData().get(0);
                String clusterName = (String) clusterInfo.get("cluster_name");
                return clusterName != null ? List.of(clusterName) : List.of();
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return List.of();
    }
    
    @Override
    public List<String> getSchemas(DataSourceConnection connection, String database) {
        // Elasticsearch没有模式概念
        return List.of();
    }
    
    @Override
    public List<DataSourceConnection.TableInfo> getTables(DataSourceConnection connection, String database, String schema) {
        try {
            // 获取所有索引
            DataSourceConnection.QueryResult result = connection.executeQuery("GET /_cat/indices?format=json", Map.of());
            
            if (result.isSuccess()) {
                List<DataSourceConnection.TableInfo> indices = new ArrayList<>();
                for (Map<String, Object> row : result.getData()) {
                    String indexName = (String) row.get("index");
                    String health = (String) row.get("health");
                    if (indexName != null && !indexName.startsWith(".")) { // 过滤系统索引
                        indices.add(new EsTableInfo(indexName, "INDEX", health));
                    }
                }
                return indices;
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return List.of();
    }
    
    @Override
    public DataSourceConnection.TableSchema getTableSchema(DataSourceConnection connection, String tableName, String database, String schema) {
        try {
            // 获取索引映射
            String query = String.format("GET /%s/_mapping", tableName);
            DataSourceConnection.QueryResult result = connection.executeQuery(query, Map.of());
            
            List<DataSourceConnection.ColumnInfo> columns = new ArrayList<>();
            
            if (result.isSuccess() && !result.getData().isEmpty()) {
                Map<String, Object> mapping = result.getData().get(0);
                
                // 解析映射结构
                @SuppressWarnings("unchecked")
                Map<String, Object> indexMapping = (Map<String, Object>) mapping.get(tableName);
                if (indexMapping != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mappings = (Map<String, Object>) indexMapping.get("mappings");
                    if (mappings != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> properties = (Map<String, Object>) mappings.get("properties");
                        if (properties != null) {
                            parseProperties(properties, "", columns);
                        }
                    }
                }
            }
            
            return new EsTableSchema(tableName, columns, List.of());
            
        } catch (Exception e) {
            throw new RuntimeException("获取Elasticsearch索引映射失败", e);
        }
    }
    
    @Override
    public Map<String, Object> getTableStatistics(DataSourceConnection connection, String tableName, String database, String schema) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取索引统计信息
            String statsQuery = String.format("GET /%s/_stats", tableName);
            DataSourceConnection.QueryResult result = connection.executeQuery(statsQuery, Map.of());
            
            if (result.isSuccess() && !result.getData().isEmpty()) {
                Map<String, Object> statsData = result.getData().get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> indices = (Map<String, Object>) statsData.get("indices");
                if (indices != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> indexStats = (Map<String, Object>) indices.get(tableName);
                    if (indexStats != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> primaries = (Map<String, Object>) indexStats.get("primaries");
                        if (primaries != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> docs = (Map<String, Object>) primaries.get("docs");
                            if (docs != null) {
                                stats.put("documentCount", docs.get("count"));
                                stats.put("deletedCount", docs.get("deleted"));
                            }
                            
                            @SuppressWarnings("unchecked")
                            Map<String, Object> store = (Map<String, Object>) primaries.get("store");
                            if (store != null) {
                                stats.put("sizeInBytes", store.get("size_in_bytes"));
                            }
                        }
                    }
                }
            }
            
            stats.put("indexName", tableName);
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getTableSampleData(DataSourceConnection connection, String tableName, String database, String schema, int limit) {
        try {
            String sampleQuery = String.format("GET /%s/_search?size=%d", tableName, limit);
            DataSourceConnection.QueryResult result = connection.executeQuery(sampleQuery, Map.of());
            
            if (result.isSuccess() && !result.getData().isEmpty()) {
                Map<String, Object> searchResult = result.getData().get(0);
                @SuppressWarnings("unchecked")
                Map<String, Object> hits = (Map<String, Object>) searchResult.get("hits");
                if (hits != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.get("hits");
                    if (hitsList != null) {
                        List<Map<String, Object>> documents = new ArrayList<>();
                        for (Map<String, Object> hit : hitsList) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> source = (Map<String, Object>) hit.get("_source");
                            if (source != null) {
                                documents.add(source);
                            }
                        }
                        return documents;
                    }
                }
            }
            
        } catch (Exception e) {
            // 忽略异常
        }
        
        return List.of();
    }
    
    @Override
    public boolean tableExists(DataSourceConnection connection, String tableName, String database, String schema) {
        try {
            String query = String.format("HEAD /%s", tableName);
            DataSourceConnection.QueryResult result = connection.executeQuery(query, Map.of());
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }
    
    @SuppressWarnings("unchecked")
    private void parseProperties(Map<String, Object> properties, String prefix, List<DataSourceConnection.ColumnInfo> columns) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String fieldName = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Map<String, Object> fieldDef = (Map<String, Object>) entry.getValue();
            
            String fieldType = (String) fieldDef.get("type");
            if (fieldType != null) {
                columns.add(new EsColumnInfo(fieldName, fieldType, true, null, null));
            }
            
            // 处理嵌套对象
            Map<String, Object> nestedProperties = (Map<String, Object>) fieldDef.get("properties");
            if (nestedProperties != null) {
                parseProperties(nestedProperties, fieldName, columns);
            }
        }
    }
    
    // 内部实现类
    private static class EsTableInfo implements DataSourceConnection.TableInfo {
        private final String name;
        private final String type;
        private final String comment;
        
        public EsTableInfo(String name, String type, String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getType() {
            return type;
        }
        
        @Override
        public String getComment() {
            return comment;
        }
    }
    
    private static class EsColumnInfo implements DataSourceConnection.ColumnInfo {
        private final String name;
        private final String type;
        private final boolean nullable;
        private final String comment;
        private final Object defaultValue;
        
        public EsColumnInfo(String name, String type, boolean nullable, String comment, Object defaultValue) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.comment = comment;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getType() {
            return type;
        }
        
        @Override
        public boolean isNullable() {
            return nullable;
        }
        
        @Override
        public String getComment() {
            return comment;
        }
        
        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }
    }
    
    private static class EsTableSchema implements DataSourceConnection.TableSchema {
        private final String tableName;
        private final List<DataSourceConnection.ColumnInfo> columns;
        private final List<DataSourceConnection.IndexInfo> indexes;
        
        public EsTableSchema(String tableName, List<DataSourceConnection.ColumnInfo> columns, List<DataSourceConnection.IndexInfo> indexes) {
            this.tableName = tableName;
            this.columns = columns;
            this.indexes = indexes;
        }
        
        @Override
        public String getTableName() {
            return tableName;
        }
        
        @Override
        public List<DataSourceConnection.ColumnInfo> getColumns() {
            return columns;
        }
        
        @Override
        public List<DataSourceConnection.IndexInfo> getIndexes() {
            return indexes;
        }
    }
}