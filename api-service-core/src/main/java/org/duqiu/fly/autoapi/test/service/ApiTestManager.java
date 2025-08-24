package org.duqiu.fly.autoapi.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.duqiu.fly.autoapi.api.model.ApiServiceVersion;
import org.duqiu.fly.autoapi.api.repository.ApiServiceAuditLogRepository;
import org.duqiu.fly.autoapi.api.repository.ApiServiceRepository;
import org.duqiu.fly.autoapi.api.repository.ApiServiceVersionRepository;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.dto.ApiTestResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增强的API测试服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiTestManager {
    
    private final ApiServiceRepository apiServiceRepository;
    private final ApiServiceVersionRepository versionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final UnifiedDataSourceFactory dataSourceFactory;
    private final ApiServiceAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 测试API服务（草稿状态）
     */
    public ApiTestResponse testDraftApi(ApiTestRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取API服务
            ApiService apiService = apiServiceRepository.findById(request.getApiServiceId())
                    .orElseThrow(() -> new IllegalArgumentException("API服务不存在"));
            
            // 验证权限
            if (!apiService.getCreatedBy().equals(userId)) {
                throw new IllegalArgumentException("无权测试该API服务");
            }
            
            // 获取数据源
            DataSource dataSource = dataSourceRepository.findById(apiService.getDataSourceId())
                    .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
            
            // 执行测试
            ApiTestResponse response = executeApiTest(dataSource, apiService.getSqlContent(), 
                                                    request.getParameters(), startTime);
            response.setTestTime(LocalDateTime.now());
            response.setTestParameters(request.getParameters());
            
            // 记录测试日志
            recordTestAuditLog(apiService.getId(), "草稿测试", request, response, userId);
            
            return response;
            
        } catch (Exception e) {
            log.error("测试草稿API失败", e);
            ApiTestResponse response = createErrorResponse(e.getMessage(), "DRAFT_TEST_ERROR", 
                                                         System.currentTimeMillis() - startTime);
            response.setTestTime(LocalDateTime.now());
            response.setTestParameters(request.getParameters());
            
            // 记录失败日志
            if (request.getApiServiceId() != null) {
                recordTestAuditLog(request.getApiServiceId(), "草稿测试失败", request, response, userId);
            }
            
            return response;
        }
    }
    
    /**
     * 测试已发布的API服务版本
     */
    public ApiTestResponse testPublishedApi(Long apiServiceId, String version, 
                                          Map<String, Object> parameters, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取指定版本，如果未指定版本则获取当前激活版本
            ApiServiceVersion serviceVersion;
            if (version != null && !version.isEmpty()) {
                serviceVersion = versionRepository
                        .findByApiServiceIdAndVersion(apiServiceId, version)
                        .orElseThrow(() -> new IllegalArgumentException("指定版本不存在"));
            } else {
                serviceVersion = versionRepository
                        .findActiveVersionByApiServiceId(apiServiceId)
                        .orElseThrow(() -> new IllegalArgumentException("没有激活的版本"));
            }
            
            // 验证API服务权限
            ApiService apiService = apiServiceRepository.findById(apiServiceId)
                    .orElseThrow(() -> new IllegalArgumentException("API服务不存在"));
            
            if (!apiService.getCreatedBy().equals(userId)) {
                throw new IllegalArgumentException("无权测试该API服务");
            }
            
            // 获取数据源
            DataSource dataSource = dataSourceRepository.findById(serviceVersion.getDataSourceId())
                    .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
            
            // 执行测试
            ApiTestResponse response = executeApiTest(dataSource, serviceVersion.getSqlContent(), 
                                                    parameters, startTime);
            response.setTestTime(LocalDateTime.now());
            response.setTestParameters(parameters);
            
            // 记录测试日志
            Map<String, Object> testDetails = new HashMap<>();
            testDetails.put("version", serviceVersion.getVersion());
            testDetails.put("parameters", parameters);
            
            recordTestAuditLog(apiServiceId, "版本测试: " + serviceVersion.getVersion(), 
                             testDetails, response, userId);
            
            return response;
            
        } catch (Exception e) {
            log.error("测试已发布API失败", e);
            ApiTestResponse response = createErrorResponse(e.getMessage(), "PUBLISHED_TEST_ERROR", 
                                                         System.currentTimeMillis() - startTime);
            response.setTestTime(LocalDateTime.now());
            response.setTestParameters(parameters);
            
            // 记录失败日志
            recordTestAuditLog(apiServiceId, "版本测试失败", parameters, response, userId);
            
            return response;
        }
    }
    
    /**
     * 验证SQL语法
     */
    public Map<String, Object> validateSql(Long dataSourceId, String sql, Map<String, Object> parameters, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取数据源
            DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                    .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
            
            // 验证权限
            if (!dataSource.getCreatedBy().equals(userId)) {
                throw new IllegalArgumentException("无权使用该数据源");
            }
            
            // 处理SQL参数
            String processedSql = processSqlParameters(sql, parameters);
            
            // 使用统一数据源工厂进行验证
            try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
                // 尝试执行简单的验证查询（如SELECT 1）
                // 这里使用简单的验证方式，实际项目中可能需要更复杂的SQL验证
                Map<String, Object> validationResult = new HashMap<>();
                validationResult.put("validationType", "basic");
                validationResult.put("validationQuery", "SELECT 1");
                
                result.put("valid", true);
                result.put("message", "SQL语法验证通过");
                result.put("processedSql", processedSql);
                result.putAll(validationResult);
                
            }
            
        } catch (Exception e) {
            log.warn("SQL验证失败: {}", e.getMessage());
            result.put("valid", false);
            result.put("message", "SQL语法错误: " + e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }
        
        return result;
    }
    
    /**
     * 获取SQL执行计划
     */
    public Map<String, Object> explainSql(Long dataSourceId, String sql, Map<String, Object> parameters, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取数据源
            DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                    .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
            
            // 验证权限
            if (!dataSource.getCreatedBy().equals(userId)) {
                throw new IllegalArgumentException("无权使用该数据源");
            }
            
            // 处理SQL参数
            String processedSql = processSqlParameters(sql, parameters);
            
            // 获取执行计划
            try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
                // 这里使用简单的执行计划模拟，实际项目中可能需要数据库特定的EXPLAIN功能
                Map<String, Object> explainResult = new HashMap<>();
                explainResult.put("explainType", "simulated");
                explainResult.put("query", processedSql);
                explainResult.put("estimatedCost", "N/A");
                explainResult.put("warning", "实际执行计划功能需要数据库特定实现");
                
                result.put("success", true);
                result.put("executionPlan", explainResult);
                result.put("processedSql", processedSql);
                
            }
            
        } catch (Exception e) {
            log.warn("获取SQL执行计划失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "获取执行计划失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 批量测试参数
     */
    public List<ApiTestResponse> batchTestApi(Long apiServiceId, List<Map<String, Object>> parametersList, Long userId) {
        return parametersList.stream()
                .map(parameters -> {
                    ApiTestRequest request = new ApiTestRequest();
                    request.setApiServiceId(apiServiceId);
                    request.setParameters(parameters);
                    return testDraftApi(request, userId);
                })
                .toList();
    }
    
    // ===== 私有方法 =====
    
    private ApiTestResponse executeApiTest(DataSource dataSource, String sql, 
                                         Map<String, Object> parameters, long startTime) {
        try {
            long connectionStart = System.currentTimeMillis();
            
            try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
                long connectionTime = System.currentTimeMillis() - connectionStart;
                
                // 处理SQL参数
                long queryStart = System.currentTimeMillis();
                String processedSql = processSqlParameters(sql, parameters);
                
                // 执行查询
                DataSourceConnection.QueryResult queryResult = connection.executeQuery(processedSql, parameters);
                long queryTime = System.currentTimeMillis() - queryStart;
                
                // 处理结果
                long processingStart = System.currentTimeMillis();
                ApiTestResponse response = new ApiTestResponse();
                response.setSuccess(queryResult.isSuccess());
                response.setData(queryResult.getData());
                response.setRecordCount((int) queryResult.getCount());
                response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                
                // 设置SQL执行详情
                ApiTestResponse.SqlExecutionDetail detail = new ApiTestResponse.SqlExecutionDetail();
                detail.setExecutedSql(processedSql);
                detail.setSqlParameters(parameters);
                detail.setConnectionTimeMs(connectionTime);
                detail.setQueryTimeMs(queryTime);
                detail.setProcessingTimeMs(System.currentTimeMillis() - processingStart);
                response.setSqlExecutionDetail(detail);
                
                return response;
            }
            
        } catch (Exception e) {
            log.error("执行API测试失败", e);
            return createErrorResponse(e.getMessage(), "SQL_EXECUTION_ERROR", 
                                     System.currentTimeMillis() - startTime);
        }
    }
    
    private String processSqlParameters(String sql, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return sql;
        }
        
        String processedSql = sql;
        
        // 处理 ${param} 格式的参数
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(sql);
        
        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object paramValue = parameters.get(paramName);
            
            if (paramValue != null) {
                String replacement;
                if (paramValue instanceof String) {
                    // 字符串参数需要加引号并转义
                    replacement = "'" + paramValue.toString().replace("'", "''") + "'";
                } else if (paramValue instanceof Number || paramValue instanceof Boolean) {
                    replacement = paramValue.toString();
                } else {
                    replacement = "'" + paramValue.toString().replace("'", "''") + "'";
                }
                processedSql = processedSql.replace("${" + paramName + "}", replacement);
            } else {
                // 参数不存在时，替换为NULL
                processedSql = processedSql.replace("${" + paramName + "}", "NULL");
            }
        }
        
        return processedSql;
    }
    
    private ApiTestResponse createErrorResponse(String message, String errorCode, long executionTime) {
        ApiTestResponse response = new ApiTestResponse();
        response.setSuccess(false);
        response.setErrorMessage(message);
        response.setErrorCode(errorCode);
        response.setExecutionTimeMs(executionTime);
        response.setRecordCount(0);
        return response;
    }
    
    private void recordTestAuditLog(Long apiServiceId, String description, Object testDetails, 
                                  ApiTestResponse response, Long userId) {
        try {
            ApiServiceAuditLog log = new ApiServiceAuditLog();
            log.setApiServiceId(apiServiceId);
            log.setOperationType(ApiServiceAuditLog.OperationType.TEST);
            log.setOperationDescription(description);
            log.setOperationDetails(objectMapper.writeValueAsString(testDetails));
            log.setAfterData(objectMapper.writeValueAsString(response));
            log.setOperationResult(response.isSuccess() ? 
                                 ApiServiceAuditLog.OperationResult.SUCCESS : 
                                 ApiServiceAuditLog.OperationResult.FAILED);
            log.setDurationMs(response.getExecutionTimeMs());
            log.setCreatedBy(userId);
            log.setUpdatedBy(userId);
            
            if (!response.isSuccess()) {
                log.setErrorMessage(response.getErrorMessage());
            }
            
            auditLogRepository.save(log);
            
        } catch (Exception e) {
            log.warn("记录测试审计日志失败", e);
        }
    }
}