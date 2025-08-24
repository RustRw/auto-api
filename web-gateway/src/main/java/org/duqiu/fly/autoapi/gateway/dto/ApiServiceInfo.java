package org.duqiu.fly.autoapi.gateway.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * API服务信息DTO
 */
public class ApiServiceInfo {
    
    /**
     * 服务唯一ID
     */
    private Long serviceId;
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 服务URL路径
     */
    private String servicePath;
    
    /**
     * HTTP方法
     */
    private String httpMethod;
    
    /**
     * 服务版本
     */
    private String version;
    
    /**
     * 是否激活
     */
    private Boolean isActive;
    
    /**
     * SQL内容
     */
    private String sqlContent;
    
    /**
     * 数据源ID
     */
    private Long dataSourceId;
    
    /**
     * 数据源配置信息
     */
    private Map<String, Object> dataSourceConfig;
    
    /**
     * 服务创建者
     */
    private String createdBy;
    
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    /**
     * 服务描述
     */
    private String description;
    
    public ApiServiceInfo() {}
    
    public ApiServiceInfo(Long serviceId, String serviceName, String servicePath, 
                         String httpMethod, String version, Boolean isActive, 
                         String sqlContent, Long dataSourceId) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.servicePath = servicePath;
        this.httpMethod = httpMethod;
        this.version = version;
        this.isActive = isActive;
        this.sqlContent = sqlContent;
        this.dataSourceId = dataSourceId;
    }
    
    // Getters and Setters
    public Long getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getServicePath() {
        return servicePath;
    }
    
    public void setServicePath(String servicePath) {
        this.servicePath = servicePath;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getSqlContent() {
        return sqlContent;
    }
    
    public void setSqlContent(String sqlContent) {
        this.sqlContent = sqlContent;
    }
    
    public Long getDataSourceId() {
        return dataSourceId;
    }
    
    public void setDataSourceId(Long dataSourceId) {
        this.dataSourceId = dataSourceId;
    }
    
    public Map<String, Object> getDataSourceConfig() {
        return dataSourceConfig;
    }
    
    public void setDataSourceConfig(Map<String, Object> dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 生成服务唯一标识键
     */
    public String getServiceKey() {
        return String.format("%s:%s:%s", httpMethod, servicePath, version);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiServiceInfo that = (ApiServiceInfo) o;
        return Objects.equals(serviceId, that.serviceId) &&
               Objects.equals(version, that.version);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(serviceId, version);
    }
    
    @Override
    public String toString() {
        return "ApiServiceInfo{" +
               "serviceId=" + serviceId +
               ", serviceName='" + serviceName + '\'' +
               ", servicePath='" + servicePath + '\'' +
               ", httpMethod='" + httpMethod + '\'' +
               ", version='" + version + '\'' +
               ", isActive=" + isActive +
               '}';
    }
}