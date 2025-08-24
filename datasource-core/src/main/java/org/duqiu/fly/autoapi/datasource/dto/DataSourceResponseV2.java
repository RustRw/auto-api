package org.duqiu.fly.autoapi.datasource.dto;

import lombok.Data;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceCategory;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceProtocol;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DataSourceResponseV2 {
    private Long id;
    private String name;
    private String description;
    private DataSourceType type;
    private DataSourceCategory category;
    private DataSourceProtocol protocol;
    private String host;
    private Integer port;
    private String database;
    private String username;
    private String version;
    private List<String> supportedVersions;
    private Integer maxPoolSize;
    private Integer minPoolSize;
    private Integer connectionTimeout;
    private Integer idleTimeout;
    private Integer maxLifetime;
    private Boolean enabled;
    private Boolean sslEnabled;
    private Boolean connectionPoolEnabled;
    private String testQuery;
    private Map<String, Object> additionalProperties;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 连接状态信息
    private ConnectionStatus connectionStatus;
    
    // 依赖信息
    private DependencyInfo dependencyInfo;
    
    @Data
    public static class ConnectionStatus {
        private Boolean connected;
        private String message;
        private LocalDateTime lastTestTime;
        private Long responseTime;
        
        public boolean isConnected() {
            return connected != null && connected;
        }
    }
    
    @Data
    public static class DependencyInfo {
        private String coordinate;
        private Boolean available;
        private String recommendedVersion;
        private List<String> supportedVersions;
        private String installCommand;
    }
}