package org.duqiu.fly.autoapi.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 服务请求日志DTO
 * 记录API服务的请求详细信息
 */
public class ServiceRequestLog {
    
    /**
     * 日志唯一ID
     */
    private String logId;
    
    /**
     * 服务类型
     */
    private String serviceType;
    
    /**
     * 服务唯一ID
     */
    private Long serviceId;
    
    /**
     * 服务URL
     */
    private String serviceUrl;
    
    /**
     * 服务版本
     */
    private String serviceVersion;
    
    /**
     * HTTP方法
     */
    private String httpMethod;
    
    /**
     * 请求参数
     */
    private Map<String, Object> requestParameters;
    
    /**
     * 请求开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime requestStartTime;
    
    /**
     * 请求结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime requestEndTime;
    
    /**
     * 请求执行时长(毫秒)
     */
    private Long executionTimeMs;
    
    /**
     * 执行的SQL脚本完整体
     */
    private String executedSqlScript;
    
    /**
     * 请求是否成功
     */
    private Boolean isSuccess;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误堆栈
     */
    private String errorStackTrace;
    
    /**
     * 返回的记录数
     */
    private Integer recordCount;
    
    /**
     * 响应数据大小(字节)
     */
    private Long responseSizeBytes;
    
    /**
     * 客户端IP地址
     */
    private String clientIpAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 数据源连接获取时间(毫秒)
     */
    private Long connectionTimeMs;
    
    /**
     * SQL执行时间(毫秒)
     */
    private Long sqlExecutionTimeMs;
    
    /**
     * 数据处理时间(毫秒)
     */
    private Long dataProcessingTimeMs;
    
    /**
     * 请求头信息
     */
    private Map<String, String> requestHeaders;
    
    /**
     * 响应状态码
     */
    private Integer responseStatusCode;
    
    /**
     * 追踪ID
     */
    private String traceId;
    
    public ServiceRequestLog() {}
    
    public ServiceRequestLog(String serviceType, Long serviceId, String serviceUrl) {
        this.serviceType = serviceType;
        this.serviceId = serviceId;
        this.serviceUrl = serviceUrl;
        this.requestStartTime = LocalDateTime.now();
        this.logId = generateLogId();
    }
    
    /**
     * 生成日志唯一ID
     */
    private String generateLogId() {
        return String.format("%s_%s_%d", 
                serviceType, 
                requestStartTime.toString().replace(":", "").replace("-", "").replace(".", ""),
                System.nanoTime() % 1000000);
    }
    
    /**
     * 标记请求完成
     */
    public void markRequestCompleted(boolean success) {
        this.requestEndTime = LocalDateTime.now();
        this.isSuccess = success;
        if (requestStartTime != null && requestEndTime != null) {
            this.executionTimeMs = java.time.Duration.between(requestStartTime, requestEndTime).toMillis();
        }
    }
    
    /**
     * 设置错误信息
     */
    public void setError(String errorMessage, String errorStackTrace) {
        this.errorMessage = errorMessage;
        this.errorStackTrace = errorStackTrace;
        this.isSuccess = false;
    }
    
    // Getters and Setters
    public String getLogId() {
        return logId;
    }
    
    public void setLogId(String logId) {
        this.logId = logId;
    }
    
    public String getServiceType() {
        return serviceType;
    }
    
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }
    
    public Long getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getServiceUrl() {
        return serviceUrl;
    }
    
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }
    
    public String getServiceVersion() {
        return serviceVersion;
    }
    
    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public Map<String, Object> getRequestParameters() {
        return requestParameters;
    }
    
    public void setRequestParameters(Map<String, Object> requestParameters) {
        this.requestParameters = requestParameters;
    }
    
    public LocalDateTime getRequestStartTime() {
        return requestStartTime;
    }
    
    public void setRequestStartTime(LocalDateTime requestStartTime) {
        this.requestStartTime = requestStartTime;
    }
    
    public LocalDateTime getRequestEndTime() {
        return requestEndTime;
    }
    
    public void setRequestEndTime(LocalDateTime requestEndTime) {
        this.requestEndTime = requestEndTime;
    }
    
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public String getExecutedSqlScript() {
        return executedSqlScript;
    }
    
    public void setExecutedSqlScript(String executedSqlScript) {
        this.executedSqlScript = executedSqlScript;
    }
    
    public Boolean getIsSuccess() {
        return isSuccess;
    }
    
    public void setIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getErrorStackTrace() {
        return errorStackTrace;
    }
    
    public void setErrorStackTrace(String errorStackTrace) {
        this.errorStackTrace = errorStackTrace;
    }
    
    public Integer getRecordCount() {
        return recordCount;
    }
    
    public void setRecordCount(Integer recordCount) {
        this.recordCount = recordCount;
    }
    
    public Long getResponseSizeBytes() {
        return responseSizeBytes;
    }
    
    public void setResponseSizeBytes(Long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }
    
    public String getClientIpAddress() {
        return clientIpAddress;
    }
    
    public void setClientIpAddress(String clientIpAddress) {
        this.clientIpAddress = clientIpAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public Long getConnectionTimeMs() {
        return connectionTimeMs;
    }
    
    public void setConnectionTimeMs(Long connectionTimeMs) {
        this.connectionTimeMs = connectionTimeMs;
    }
    
    public Long getSqlExecutionTimeMs() {
        return sqlExecutionTimeMs;
    }
    
    public void setSqlExecutionTimeMs(Long sqlExecutionTimeMs) {
        this.sqlExecutionTimeMs = sqlExecutionTimeMs;
    }
    
    public Long getDataProcessingTimeMs() {
        return dataProcessingTimeMs;
    }
    
    public void setDataProcessingTimeMs(Long dataProcessingTimeMs) {
        this.dataProcessingTimeMs = dataProcessingTimeMs;
    }
    
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }
    
    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }
    
    public Integer getResponseStatusCode() {
        return responseStatusCode;
    }
    
    public void setResponseStatusCode(Integer responseStatusCode) {
        this.responseStatusCode = responseStatusCode;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    @Override
    public String toString() {
        return "ServiceRequestLog{" +
               "logId='" + logId + '\'' +
               ", serviceType='" + serviceType + '\'' +
               ", serviceId=" + serviceId +
               ", serviceUrl='" + serviceUrl + '\'' +
               ", httpMethod='" + httpMethod + '\'' +
               ", executionTimeMs=" + executionTimeMs +
               ", isSuccess=" + isSuccess +
               '}';
    }
}