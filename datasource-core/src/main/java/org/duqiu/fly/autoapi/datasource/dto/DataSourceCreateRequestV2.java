package org.duqiu.fly.autoapi.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;

import java.util.Map;

@Data
public class DataSourceCreateRequestV2 {
    
    @NotBlank(message = "数据源名称不能为空")
    private String name;
    
    private String description;
    
    @NotNull(message = "数据源类型不能为空")
    private DataSourceType type;
    
    @NotBlank(message = "主机地址不能为空")
    private String host;
    
    @NotNull(message = "端口不能为空")
    @Min(value = 1, message = "端口必须大于0")
    @Max(value = 65535, message = "端口不能超过65535")
    private Integer port;
    
    private String database;
    
    private String username;
    
    private String password;
    
    private String version;
    
    @Min(value = 1, message = "连接池大小至少为1")
    @Max(value = 100, message = "连接池大小不能超过100")
    private Integer maxPoolSize = 10;
    
    @Min(value = 1, message = "最小连接池大小至少为1")
    private Integer minPoolSize = 1;
    
    @Min(value = 1000, message = "连接超时时间至少为1000ms")
    private Integer connectionTimeout = 30000;
    
    @Min(value = 60000, message = "空闲超时时间至少为60000ms")
    private Integer idleTimeout = 600000;
    
    @Min(value = 300000, message = "连接最大生命周期至少为300000ms")
    private Integer maxLifetime = 1800000;
    
    private String testQuery;
    
    private Boolean sslEnabled = false;
    
    private Boolean connectionPoolEnabled = true;
    
    private Map<String, Object> additionalProperties;
    
    /**
     * 验证数据源配置的完整性
     */
    public boolean isValid() {
        if (type == null) {
            return false;
        }
        
        // 根据数据源类型验证必填字段
        switch (type.getProtocol()) {
            case JDBC:
                return username != null && !username.isEmpty();
            case HTTP:
                return true; // HTTP类型的用户名密码是可选的
            case NATIVE:
                switch (type) {
                    case MONGODB:
                        return database != null && !database.isEmpty();
                    case ELASTICSEARCH:
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }
    
    /**
     * 获取数据源的默认测试查询
     */
    public String getDefaultTestQuery() {
        if (testQuery != null && !testQuery.isEmpty()) {
            return testQuery;
        }
        
        switch (type) {
            case MYSQL:
            case STARROCKS:
                return "SELECT 1";
            case POSTGRESQL:
                return "SELECT 1";
            case ORACLE:
                return "SELECT 1 FROM DUAL";
            case CLICKHOUSE:
                return "SELECT 1";
            case TDENGINE:
                return "SELECT SERVER_STATUS()";
            default:
                return "SELECT 1";
        }
    }
}