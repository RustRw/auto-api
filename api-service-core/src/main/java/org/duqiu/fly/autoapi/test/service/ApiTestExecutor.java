package org.duqiu.fly.autoapi.test.service;

import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.repository.ApiServiceRepository;
import org.duqiu.fly.autoapi.api.service.ApiServiceManager;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.dto.ApiTestResponse;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ApiTestExecutor {
    
    private final ApiServiceRepository apiServiceRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ApiServiceManager apiServiceManager;
    
    public ApiTestExecutor(ApiServiceRepository apiServiceRepository,
                           DataSourceRepository dataSourceRepository,
                           ApiServiceManager apiServiceManager) {
        this.apiServiceRepository = apiServiceRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.apiServiceManager = apiServiceManager;
    }
    
    public ApiTestResponse testApi(ApiTestRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            ApiService apiService = apiServiceRepository.findById(request.getApiServiceId())
                    .orElseThrow(() -> new RuntimeException("API服务不存在"));
            
            if (!apiService.getCreatedBy().equals(userId)) {
                throw new RuntimeException("无权测试该API服务");
            }
            
            DataSource dataSource = dataSourceRepository.findById(apiService.getDataSourceId())
                    .orElseThrow(() -> new RuntimeException("数据源不存在"));
            
            Object result = executeSqlWithParams(dataSource, apiService.getSqlContent(), 
                                                request.getParameters());
            
            long executionTime = System.currentTimeMillis() - startTime;
            return ApiTestResponse.success(result, executionTime);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return ApiTestResponse.error("测试失败", e.getMessage(), executionTime);
        }
    }
    
    public ApiTestResponse testPublishedApi(String path, ApiService.HttpMethod method, 
                                          Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = apiServiceManager.executeApi(path, method, params);
            long executionTime = System.currentTimeMillis() - startTime;
            return ApiTestResponse.success(result, executionTime);
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return ApiTestResponse.error("API调用失败", e.getMessage(), executionTime);
        }
    }
    
    public Map<String, Object> validateSql(Long dataSourceId, String sql, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权使用该数据源");
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                dataSource.getPassword())) {
            
            // 执行EXPLAIN或类似操作来验证SQL
            String validateSql = "EXPLAIN " + sql;
            try (PreparedStatement statement = connection.prepareStatement(validateSql)) {
                statement.executeQuery();
                result.put("valid", true);
                result.put("message", "SQL语法正确");
            }
            
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "SQL语法错误: " + e.getMessage());
        }
        
        return result;
    }
    
    private Object executeSqlWithParams(DataSource dataSource, String sql, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                dataSource.getPassword())) {
            
            // 解析SQL中的参数占位符
            String processedSql = replaceSqlParameters(sql, params);
            
            try (PreparedStatement statement = connection.prepareStatement(processedSql)) {
                
                if (isSelectStatement(processedSql)) {
                    try (ResultSet resultSet = statement.executeQuery()) {
                        List<Map<String, Object>> rows = new ArrayList<>();
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        
                        while (resultSet.next()) {
                            Map<String, Object> row = new HashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = metaData.getColumnName(i);
                                Object value = resultSet.getObject(i);
                                row.put(columnName, value);
                            }
                            rows.add(row);
                        }
                        
                        result.put("data", rows);
                        result.put("count", rows.size());
                        result.put("columns", getColumnInfo(metaData));
                    }
                } else {
                    int updateCount = statement.executeUpdate();
                    result.put("affectedRows", updateCount);
                    result.put("message", "操作成功");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("执行SQL失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    private String replaceSqlParameters(String sql, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return sql;
        }
        
        String result = sql;
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(sql);
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object paramValue = params.get(paramName);
            if (paramValue != null) {
                String replacement = paramValue.toString();
                if (paramValue instanceof String) {
                    replacement = "'" + replacement.replace("'", "''") + "'";
                }
                result = result.replace("${" + paramName + "}", replacement);
            }
        }
        
        return result;
    }
    
    private boolean isSelectStatement(String sql) {
        return sql.trim().toLowerCase().startsWith("select");
    }
    
    private List<Map<String, Object>> getColumnInfo(ResultSetMetaData metaData) throws SQLException {
        List<Map<String, Object>> columns = new ArrayList<>();
        
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            Map<String, Object> column = new HashMap<>();
            column.put("name", metaData.getColumnName(i));
            column.put("type", metaData.getColumnTypeName(i));
            column.put("size", metaData.getColumnDisplaySize(i));
            column.put("nullable", metaData.isNullable(i) == ResultSetMetaData.columnNullable);
            columns.add(column);
        }
        
        return columns;
    }
}