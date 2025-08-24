package org.duqiu.fly.autoapi.datasource.metadata;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB元数据适配器
 */
public class MongoMetadataAdapter implements MetadataAdapter {
    
    @Override
    public List<String> getDatabases(DataSourceConnection connection) {
        try {
            DataSourceConnection.QueryResult result = connection.executeQuery("show dbs", Map.of());
            if (result.isSuccess()) {
                List<String> databases = new ArrayList<>();
                for (Map<String, Object> row : result.getData()) {
                    databases.add((String) row.get("name"));
                }
                return databases;
            }
        } catch (Exception e) {
            // 忽略异常，返回空列表
        }
        return List.of();
    }
    
    @Override
    public List<String> getSchemas(DataSourceConnection connection, String database) {
        // MongoDB不支持模式概念
        return List.of();
    }
    
    @Override
    public List<DataSourceConnection.TableInfo> getTables(DataSourceConnection connection, String database, String schema) {
        try {
            String query = database != null ? "use " + database + "; show collections" : "show collections";
            DataSourceConnection.QueryResult result = connection.executeQuery(query, Map.of());
            
            if (result.isSuccess()) {
                List<DataSourceConnection.TableInfo> collections = new ArrayList<>();
                for (Map<String, Object> row : result.getData()) {
                    String collectionName = (String) row.get("name");
                    collections.add(new MongoTableInfo(collectionName, "COLLECTION", null));
                }
                return collections;
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return List.of();
    }
    
    @Override
    public DataSourceConnection.TableSchema getTableSchema(DataSourceConnection connection, String tableName, String database, String schema) {
        try {
            // MongoDB文档结构分析
            String query = String.format("%s.%s.findOne()", 
                    database != null ? database : "db", tableName);
            
            DataSourceConnection.QueryResult result = connection.executeQuery(query, Map.of());
            
            List<DataSourceConnection.ColumnInfo> columns = new ArrayList<>();
            if (result.isSuccess() && !result.getData().isEmpty()) {
                Map<String, Object> sampleDoc = result.getData().get(0);
                for (Map.Entry<String, Object> entry : sampleDoc.entrySet()) {
                    String fieldName = entry.getKey();
                    String fieldType = getMongoFieldType(entry.getValue());
                    columns.add(new MongoColumnInfo(fieldName, fieldType, true, null, null));
                }
            }
            
            return new MongoTableSchema(tableName, columns, List.of());
            
        } catch (Exception e) {
            throw new RuntimeException("获取MongoDB集合结构失败", e);
        }
    }
    
    @Override
    public Map<String, Object> getTableStatistics(DataSourceConnection connection, String tableName, String database, String schema) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            String countQuery = String.format("db.%s.count()", tableName);
            DataSourceConnection.QueryResult result = connection.executeQuery(countQuery, Map.of());
            
            if (result.isSuccess() && !result.getData().isEmpty()) {
                Object count = result.getData().get(0).values().iterator().next();
                stats.put("documentCount", count);
            }
            
            stats.put("collectionName", tableName);
            stats.put("database", database);
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getTableSampleData(DataSourceConnection connection, String tableName, String database, String schema, int limit) {
        try {
            String sampleQuery = String.format("db.%s.find().limit(%d)", tableName, limit);
            DataSourceConnection.QueryResult result = connection.executeQuery(sampleQuery, Map.of());
            
            if (result.isSuccess()) {
                return result.getData();
            }
            
        } catch (Exception e) {
            // 忽略异常
        }
        
        return List.of();
    }
    
    @Override
    public boolean tableExists(DataSourceConnection connection, String tableName, String database, String schema) {
        try {
            List<DataSourceConnection.TableInfo> tables = getTables(connection, database, schema);
            return tables.stream().anyMatch(table -> tableName.equals(table.getName()));
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getMongoFieldType(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Number) return "number";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof List) return "array";
        if (value instanceof Map) return "object";
        return "unknown";
    }
    
    // 内部实现类
    private static class MongoTableInfo implements DataSourceConnection.TableInfo {
        private final String name;
        private final String type;
        private final String comment;
        
        public MongoTableInfo(String name, String type, String comment) {
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
    
    private static class MongoColumnInfo implements DataSourceConnection.ColumnInfo {
        private final String name;
        private final String type;
        private final boolean nullable;
        private final String comment;
        private final Object defaultValue;
        
        public MongoColumnInfo(String name, String type, boolean nullable, String comment, Object defaultValue) {
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
    
    private static class MongoTableSchema implements DataSourceConnection.TableSchema {
        private final String tableName;
        private final List<DataSourceConnection.ColumnInfo> columns;
        private final List<DataSourceConnection.IndexInfo> indexes;
        
        public MongoTableSchema(String tableName, List<DataSourceConnection.ColumnInfo> columns, List<DataSourceConnection.IndexInfo> indexes) {
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