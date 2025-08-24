package org.duqiu.fly.autoapi.datasource.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;

import java.util.Map;

/**
 * 数据源更新请求DTO
 */
public class DataSourceUpdateRequest {
    
    @NotBlank(message = "数据源名称不能为空")
    @Size(max = 100, message = "数据源名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "描述长度不能超过500个字符")
    private String description;
    
    private String host;
    
    @Min(value = 1, message = "端口号必须大于0")
    @Max(value = 65535, message = "端口号不能超过65535")
    private Integer port;
    
    private String database;
    private String username;
    private String password;
    private String version;
    
    // 连接池配置
    @Min(value = 1, message = "最大连接数不能小于1")
    @Max(value = 100, message = "最大连接数不能超过100")
    private Integer maxPoolSize;
    
    @Min(value = 0, message = "最小连接数不能小于0")
    private Integer minPoolSize;
    
    @Min(value = 1000, message = "连接超时时间不能小于1000毫秒")
    private Long connectionTimeout;
    
    @Min(value = 60000, message = "空闲超时时间不能小于60秒")
    private Long idleTimeout;
    
    @Min(value = 600000, message = "最大生命周期不能小于10分钟")
    private Long maxLifetime;
    
    // 其他配置
    private Boolean sslEnabled;
    private Boolean connectionPoolEnabled;
    private Boolean enabled;
    
    // 额外属性
    private Map<String, Object> additionalProperties;
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    
    public Integer getMinPoolSize() {
        return minPoolSize;
    }
    
    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }
    
    public Long getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(Long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public Long getIdleTimeout() {
        return idleTimeout;
    }
    
    public void setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }
    
    public Long getMaxLifetime() {
        return maxLifetime;
    }
    
    public void setMaxLifetime(Long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }
    
    public Boolean getSslEnabled() {
        return sslEnabled;
    }
    
    public void setSslEnabled(Boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
    }
    
    public Boolean getConnectionPoolEnabled() {
        return connectionPoolEnabled;
    }
    
    public void setConnectionPoolEnabled(Boolean connectionPoolEnabled) {
        this.connectionPoolEnabled = connectionPoolEnabled;
    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
    
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}