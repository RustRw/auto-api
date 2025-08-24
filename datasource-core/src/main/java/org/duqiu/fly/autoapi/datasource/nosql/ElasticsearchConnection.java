package org.duqiu.fly.autoapi.datasource.nosql;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.*;

/**
 * Elasticsearch连接实现
 */
public class ElasticsearchConnection implements DataSourceConnection {
    
    private final String baseUrl;
    private final String username;
    private final String password;
    private final RestTemplate restTemplate;
    private final HttpHeaders headers;
    
    public ElasticsearchConnection(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.username = username;
        this.password = password;
        this.restTemplate = new RestTemplate();
        this.headers = new HttpHeaders();
        
        // 设置认证
        if (username != null && !username.isEmpty()) {
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
        }
        
        headers.setContentType(MediaType.APPLICATION_JSON);
    }
    
    @Override
    public boolean isValid() {
        try {
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl, HttpMethod.GET, entity, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public QueryResult executeQuery(String query, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        try {
            ESQuery esQuery = parseESQuery(query, parameters);
            
            String searchUrl = baseUrl + "/" + esQuery.getIndex() + "/_search";
            HttpEntity<String> entity = new HttpEntity<>(esQuery.getQueryBody(), headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                searchUrl, HttpMethod.POST, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> data = extractDataFromESResponse(responseBody);
                List<ColumnInfo> columns = extractColumnsFromData(data);
                
                long executionTime = System.currentTimeMillis() - startTime;
                long totalHits = getTotalHits(responseBody);
                
                return new ESQueryResult(data, totalHits, columns, executionTime, true, null);
            } else {
                long executionTime = System.currentTimeMillis() - startTime;
                return new ESQueryResult(Collections.emptyList(), 0, Collections.emptyList(),
                                        executionTime, false, "HTTP " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new ESQueryResult(Collections.emptyList(), 0, Collections.emptyList(),
                                    executionTime, false, e.getMessage());
        }
    }
    
    @Override
    public UpdateResult executeUpdate(String command, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        try {
            ESUpdateCommand updateCommand = parseESUpdateCommand(command, parameters);
            
            String url = baseUrl + "/" + updateCommand.getIndex() + "/" + updateCommand.getOperation();
            HttpEntity<String> entity = new HttpEntity<>(updateCommand.getBody(), headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, updateCommand.getMethod(), entity, Map.class);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                long affectedRows = extractAffectedRows(responseBody);
                return new ESUpdateResult(affectedRows, executionTime, true, null);
            } else {
                return new ESUpdateResult(0, executionTime, false, "HTTP " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new ESUpdateResult(0, executionTime, false, e.getMessage());
        }
    }
    
    @Override
    public ConnectionInfo getConnectionInfo() {
        try {
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> version = (Map<String, Object>) responseBody.get("version");
                
                Map<String, Object> properties = new HashMap<>();
                properties.put("name", responseBody.get("name"));
                properties.put("cluster_name", responseBody.get("cluster_name"));
                properties.put("tagline", responseBody.get("tagline"));
                
                return new ESConnectionInfo(baseUrl, (String) version.get("number"), properties);
            }
        } catch (Exception e) {
            // 日志记录错误
        }
        
        return new ESConnectionInfo(baseUrl, "unknown", Collections.emptyMap());
    }
    
    @Override
    public List<TableInfo> getTables() {
        List<TableInfo> tables = new ArrayList<>();
        try {
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/_cat/indices?format=json", HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> indices = (List<Map<String, Object>>) response.getBody();
                for (Map<String, Object> index : indices) {
                    String indexName = (String) index.get("index");
                    if (!indexName.startsWith(".")) { // 跳过系统索引
                        tables.add(new ESTableInfo(indexName, "INDEX", null));
                    }
                }
            }
        } catch (Exception e) {
            // 日志记录错误
        }
        return tables;
    }
    
    @Override
    public TableSchema getTableSchema(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        
        try {
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/" + tableName + "/_mapping", HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> indexMapping = (Map<String, Object>) responseBody.get(tableName);
                Map<String, Object> mappings = (Map<String, Object>) indexMapping.get("mappings");
                Map<String, Object> properties = (Map<String, Object>) mappings.get("properties");
                
                extractFieldsFromMapping(properties, "", columns);
            }
        } catch (Exception e) {
            // 日志记录错误
        }
        
        return new ESTableSchema(tableName, columns, Collections.emptyList());
    }
    
    @Override
    public void close() {
        // Elasticsearch连接是无状态的，不需要显式关闭
    }
    
    private List<Map<String, Object>> extractDataFromESResponse(Map<String, Object> responseBody) {
        List<Map<String, Object>> data = new ArrayList<>();
        
        Map<String, Object> hits = (Map<String, Object>) responseBody.get("hits");
        List<Map<String, Object>> hitsList = (List<Map<String, Object>>) hits.get("hits");
        
        for (Map<String, Object> hit : hitsList) {
            Map<String, Object> source = (Map<String, Object>) hit.get("_source");
            Map<String, Object> row = new HashMap<>(source);
            row.put("_id", hit.get("_id"));
            row.put("_index", hit.get("_index"));
            data.add(row);
        }
        
        return data;
    }
    
    private long getTotalHits(Map<String, Object> responseBody) {
        Map<String, Object> hits = (Map<String, Object>) responseBody.get("hits");
        Object total = hits.get("total");
        
        if (total instanceof Map) {
            return ((Number) ((Map<String, Object>) total).get("value")).longValue();
        } else if (total instanceof Number) {
            return ((Number) total).longValue();
        }
        
        return 0;
    }
    
    private List<ColumnInfo> extractColumnsFromData(List<Map<String, Object>> data) {
        Set<String> fieldNames = new HashSet<>();
        for (Map<String, Object> row : data) {
            fieldNames.addAll(row.keySet());
        }
        
        List<ColumnInfo> columns = new ArrayList<>();
        for (String fieldName : fieldNames) {
            columns.add(new ESColumnInfo(fieldName, "object", true, null, null));
        }
        return columns;
    }
    
    private void extractFieldsFromMapping(Map<String, Object> properties, String prefix, List<ColumnInfo> columns) {
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String fieldName = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Map<String, Object> fieldMapping = (Map<String, Object>) entry.getValue();
            
            String type = (String) fieldMapping.get("type");
            if (type != null) {
                columns.add(new ESColumnInfo(fieldName, type, true, null, null));
            }
            
            // 处理嵌套对象
            Map<String, Object> nestedProperties = (Map<String, Object>) fieldMapping.get("properties");
            if (nestedProperties != null) {
                extractFieldsFromMapping(nestedProperties, fieldName, columns);
            }
        }
    }
    
    private ESQuery parseESQuery(String query, Map<String, Object> parameters) {
        // 简化的ES查询解析
        ESQuery esQuery = new ESQuery();
        esQuery.setIndex("_all");
        
        Map<String, Object> queryBody = new HashMap<>();
        Map<String, Object> matchAll = new HashMap<>();
        matchAll.put("match_all", Collections.emptyMap());
        queryBody.put("query", matchAll);
        
        esQuery.setQueryBody(convertToJsonString(queryBody));
        return esQuery;
    }
    
    private ESUpdateCommand parseESUpdateCommand(String command, Map<String, Object> parameters) {
        // 简化的ES更新命令解析
        ESUpdateCommand updateCommand = new ESUpdateCommand();
        updateCommand.setIndex("test");
        updateCommand.setOperation("_doc");
        updateCommand.setMethod(HttpMethod.POST);
        updateCommand.setBody(convertToJsonString(parameters));
        return updateCommand;
    }
    
    private String convertToJsonString(Object obj) {
        // 简化的JSON转换，实际应该使用ObjectMapper
        return obj.toString();
    }
    
    private long extractAffectedRows(Map<String, Object> responseBody) {
        // 从ES响应中提取影响的行数
        if (responseBody.containsKey("created")) {
            return (Boolean) responseBody.get("created") ? 1 : 0;
        }
        return 1; // 默认值
    }
    
    // 内部类定义...
    static class ESQuery {
        private String index;
        private String queryBody;
        
        public String getIndex() { return index; }
        public void setIndex(String index) { this.index = index; }
        public String getQueryBody() { return queryBody; }
        public void setQueryBody(String queryBody) { this.queryBody = queryBody; }
    }
    
    static class ESUpdateCommand {
        private String index;
        private String operation;
        private HttpMethod method;
        private String body;
        
        public String getIndex() { return index; }
        public void setIndex(String index) { this.index = index; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public HttpMethod getMethod() { return method; }
        public void setMethod(HttpMethod method) { this.method = method; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
    
    // Result实现类...
    static class ESQueryResult implements QueryResult {
        private final List<Map<String, Object>> data;
        private final long count;
        private final List<ColumnInfo> columns;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public ESQueryResult(List<Map<String, Object>> data, long count, List<ColumnInfo> columns,
                            long executionTime, boolean success, String errorMessage) {
            this.data = data;
            this.count = count;
            this.columns = columns;
            this.executionTime = executionTime;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        @Override
        public List<Map<String, Object>> getData() { return data; }
        @Override
        public long getCount() { return count; }
        @Override
        public List<ColumnInfo> getColumns() { return columns; }
        @Override
        public long getExecutionTime() { return executionTime; }
        @Override
        public boolean isSuccess() { return success; }
        @Override
        public String getErrorMessage() { return errorMessage; }
    }
    
    static class ESUpdateResult implements UpdateResult {
        private final long affectedRows;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public ESUpdateResult(long affectedRows, long executionTime, boolean success, String errorMessage) {
            this.affectedRows = affectedRows;
            this.executionTime = executionTime;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        @Override
        public long getAffectedRows() { return affectedRows; }
        @Override
        public long getExecutionTime() { return executionTime; }
        @Override
        public boolean isSuccess() { return success; }
        @Override
        public String getErrorMessage() { return errorMessage; }
    }
    
    // 其他内部类...
    static class ESConnectionInfo implements ConnectionInfo {
        private final String url;
        private final String version;
        private final Map<String, Object> properties;
        
        public ESConnectionInfo(String url, String version, Map<String, Object> properties) {
            this.url = url;
            this.version = version;
            this.properties = properties;
        }
        
        @Override
        public String getUrl() { return url; }
        @Override
        public String getVersion() { return version; }
        @Override
        public Map<String, Object> getProperties() { return properties; }
    }
    
    static class ESTableInfo implements TableInfo {
        private final String name;
        private final String type;
        private final String comment;
        
        public ESTableInfo(String name, String type, String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }
        
        @Override
        public String getName() { return name; }
        @Override
        public String getType() { return type; }
        @Override
        public String getComment() { return comment; }
    }
    
    static class ESColumnInfo implements ColumnInfo {
        private final String name;
        private final String type;
        private final boolean nullable;
        private final String comment;
        private final Object defaultValue;
        
        public ESColumnInfo(String name, String type, boolean nullable, String comment, Object defaultValue) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.comment = comment;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public String getName() { return name; }
        @Override
        public String getType() { return type; }
        @Override
        public boolean isNullable() { return nullable; }
        @Override
        public String getComment() { return comment; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    
    static class ESTableSchema implements TableSchema {
        private final String tableName;
        private final List<ColumnInfo> columns;
        private final List<IndexInfo> indexes;
        
        public ESTableSchema(String tableName, List<ColumnInfo> columns, List<IndexInfo> indexes) {
            this.tableName = tableName;
            this.columns = columns;
            this.indexes = indexes;
        }
        
        @Override
        public String getTableName() { return tableName; }
        @Override
        public List<ColumnInfo> getColumns() { return columns; }
        @Override
        public List<IndexInfo> getIndexes() { return indexes; }
    }
}