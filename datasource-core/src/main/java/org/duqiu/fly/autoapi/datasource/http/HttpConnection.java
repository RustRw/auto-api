package org.duqiu.fly.autoapi.datasource.http;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * HTTP API数据源连接实现
 */
public class HttpConnection implements DataSourceConnection {
    
    private final String baseUrl;
    private final String username;
    private final String password;
    private final RestTemplate restTemplate;
    private final HttpHeaders headers;
    
    public HttpConnection(String baseUrl, String username, String password) {
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
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl, HttpMethod.HEAD, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // 如果HEAD不支持，尝试GET
            try {
                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl, HttpMethod.GET, entity, String.class);
                return response.getStatusCode().is2xxSuccessful();
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    @Override
    public QueryResult executeQuery(String query, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        try {
            HttpRequest httpRequest = parseHttpQuery(query, parameters);
            
            String url = baseUrl + httpRequest.getPath();
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.putAll(headers);
            
            // 添加自定义header
            if (httpRequest.getHeaders() != null) {
                for (Map.Entry<String, String> entry : httpRequest.getHeaders().entrySet()) {
                    requestHeaders.set(entry.getKey(), entry.getValue());
                }
            }
            
            HttpEntity<String> entity;
            if (httpRequest.getMethod() == HttpMethod.GET) {
                // GET请求：参数放在URL中
                if (httpRequest.getQueryParams() != null && !httpRequest.getQueryParams().isEmpty()) {
                    StringBuilder queryString = new StringBuilder("?");
                    for (Map.Entry<String, String> entry : httpRequest.getQueryParams().entrySet()) {
                        queryString.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                    }
                    url += queryString.substring(0, queryString.length() - 1);
                }
                entity = new HttpEntity<>(requestHeaders);
            } else {
                // POST/PUT等请求：参数放在body中
                entity = new HttpEntity<>(httpRequest.getBody(), requestHeaders);
            }
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url, httpRequest.getMethod(), entity, Object.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                List<Map<String, Object>> data = processHttpResponse(response.getBody());
                List<ColumnInfo> columns = extractColumnsFromData(data);
                
                long executionTime = System.currentTimeMillis() - startTime;
                return new HttpQueryResult(data, data.size(), columns, executionTime, true, null);
            } else {
                long executionTime = System.currentTimeMillis() - startTime;
                return new HttpQueryResult(Collections.emptyList(), 0, Collections.emptyList(),
                                          executionTime, false, "HTTP " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new HttpQueryResult(Collections.emptyList(), 0, Collections.emptyList(),
                                      executionTime, false, e.getMessage());
        }
    }
    
    @Override
    public UpdateResult executeUpdate(String command, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        try {
            HttpRequest httpRequest = parseHttpUpdateCommand(command, parameters);
            
            String url = baseUrl + httpRequest.getPath();
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.putAll(headers);
            
            if (httpRequest.getHeaders() != null) {
                for (Map.Entry<String, String> entry : httpRequest.getHeaders().entrySet()) {
                    requestHeaders.set(entry.getKey(), entry.getValue());
                }
            }
            
            HttpEntity<String> entity = new HttpEntity<>(httpRequest.getBody(), requestHeaders);
            
            ResponseEntity<Object> response = restTemplate.exchange(
                url, httpRequest.getMethod(), entity, Object.class);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful()) {
                long affectedRows = extractAffectedRows(response.getBody());
                return new HttpUpdateResult(affectedRows, executionTime, true, null);
            } else {
                return new HttpUpdateResult(0, executionTime, false, "HTTP " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new HttpUpdateResult(0, executionTime, false, e.getMessage());
        }
    }
    
    @Override
    public ConnectionInfo getConnectionInfo() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("baseUrl", baseUrl);
        properties.put("protocol", baseUrl.startsWith("https") ? "HTTPS" : "HTTP");
        properties.put("authenticated", username != null && !username.isEmpty());
        
        return new HttpConnectionInfo(baseUrl, "1.1", properties);
    }
    
    @Override
    public List<TableInfo> getTables() {
        // HTTP API没有传统意义上的表概念，返回可用的端点
        List<TableInfo> tables = new ArrayList<>();
        
        // 尝试获取API文档或端点列表
        try {
            String[] commonEndpoints = {"/api", "/v1", "/docs", "/swagger.json", "/openapi.json"};
            for (String endpoint : commonEndpoints) {
                try {
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    ResponseEntity<String> response = restTemplate.exchange(
                        baseUrl + endpoint, HttpMethod.GET, entity, String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        tables.add(new HttpTableInfo(endpoint, "ENDPOINT", "API Endpoint"));
                    }
                } catch (Exception e) {
                    // 忽略单个端点的错误
                }
            }
        } catch (Exception e) {
            // 日志记录错误
        }
        
        if (tables.isEmpty()) {
            tables.add(new HttpTableInfo("/", "ENDPOINT", "Root Endpoint"));
        }
        
        return tables;
    }
    
    @Override
    public TableSchema getTableSchema(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        
        // 尝试通过OPTIONS请求获取API schema
        try {
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + tableName, HttpMethod.OPTIONS, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // 解析响应header获取支持的方法
                HttpHeaders responseHeaders = response.getHeaders();
                List<String> allowedMethods = responseHeaders.get("Allow");
                if (allowedMethods != null) {
                    columns.add(new HttpColumnInfo("allowed_methods", "string", false, 
                                                  "Allowed HTTP Methods", String.join(",", allowedMethods)));
                }
            }
        } catch (Exception e) {
            // 添加默认列信息
            columns.add(new HttpColumnInfo("response", "object", true, "API Response", null));
        }
        
        return new HttpTableSchema(tableName, columns, Collections.emptyList());
    }
    
    @Override
    public void close() {
        // HTTP连接是无状态的，不需要显式关闭
    }
    
    private List<Map<String, Object>> processHttpResponse(Object responseBody) {
        List<Map<String, Object>> data = new ArrayList<>();
        
        if (responseBody instanceof List) {
            List<?> list = (List<?>) responseBody;
            for (Object item : list) {
                if (item instanceof Map) {
                    data.add((Map<String, Object>) item);
                } else {
                    Map<String, Object> row = new HashMap<>();
                    row.put("value", item);
                    data.add(row);
                }
            }
        } else if (responseBody instanceof Map) {
            data.add((Map<String, Object>) responseBody);
        } else {
            Map<String, Object> row = new HashMap<>();
            row.put("response", responseBody);
            data.add(row);
        }
        
        return data;
    }
    
    private List<ColumnInfo> extractColumnsFromData(List<Map<String, Object>> data) {
        Set<String> fieldNames = new HashSet<>();
        for (Map<String, Object> row : data) {
            fieldNames.addAll(row.keySet());
        }
        
        List<ColumnInfo> columns = new ArrayList<>();
        for (String fieldName : fieldNames) {
            columns.add(new HttpColumnInfo(fieldName, "object", true, null, null));
        }
        return columns;
    }
    
    private HttpRequest parseHttpQuery(String query, Map<String, Object> parameters) {
        // 简化的HTTP查询解析
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod(HttpMethod.GET);
        httpRequest.setPath("/");
        
        if (parameters != null) {
            Map<String, String> queryParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                queryParams.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            httpRequest.setQueryParams(queryParams);
        }
        
        return httpRequest;
    }
    
    private HttpRequest parseHttpUpdateCommand(String command, Map<String, Object> parameters) {
        // 简化的HTTP更新命令解析
        HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod(HttpMethod.POST);
        httpRequest.setPath("/");
        httpRequest.setBody(convertToJsonString(parameters));
        return httpRequest;
    }
    
    private String convertToJsonString(Object obj) {
        // 简化的JSON转换
        return obj != null ? obj.toString() : "{}";
    }
    
    private long extractAffectedRows(Object responseBody) {
        // 从HTTP响应中提取影响的行数
        if (responseBody instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) responseBody;
            if (map.containsKey("affected_rows")) {
                return ((Number) map.get("affected_rows")).longValue();
            }
            if (map.containsKey("count")) {
                return ((Number) map.get("count")).longValue();
            }
        }
        return 1; // 默认值
    }
    
    // 内部类定义...
    static class HttpRequest {
        private HttpMethod method;
        private String path;
        private Map<String, String> headers;
        private Map<String, String> queryParams;
        private String body;
        
        // getters and setters
        public HttpMethod getMethod() { return method; }
        public void setMethod(HttpMethod method) { this.method = method; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public Map<String, String> getQueryParams() { return queryParams; }
        public void setQueryParams(Map<String, String> queryParams) { this.queryParams = queryParams; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
    
    // Result实现类和其他内部类...
    static class HttpQueryResult implements QueryResult {
        private final List<Map<String, Object>> data;
        private final long count;
        private final List<ColumnInfo> columns;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public HttpQueryResult(List<Map<String, Object>> data, long count, List<ColumnInfo> columns,
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
    
    static class HttpUpdateResult implements UpdateResult {
        private final long affectedRows;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public HttpUpdateResult(long affectedRows, long executionTime, boolean success, String errorMessage) {
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
    static class HttpConnectionInfo implements ConnectionInfo {
        private final String url;
        private final String version;
        private final Map<String, Object> properties;
        
        public HttpConnectionInfo(String url, String version, Map<String, Object> properties) {
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
    
    static class HttpTableInfo implements TableInfo {
        private final String name;
        private final String type;
        private final String comment;
        
        public HttpTableInfo(String name, String type, String comment) {
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
    
    static class HttpColumnInfo implements ColumnInfo {
        private final String name;
        private final String type;
        private final boolean nullable;
        private final String comment;
        private final Object defaultValue;
        
        public HttpColumnInfo(String name, String type, boolean nullable, String comment, Object defaultValue) {
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
    
    static class HttpTableSchema implements TableSchema {
        private final String tableName;
        private final List<ColumnInfo> columns;
        private final List<IndexInfo> indexes;
        
        public HttpTableSchema(String tableName, List<ColumnInfo> columns, List<IndexInfo> indexes) {
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