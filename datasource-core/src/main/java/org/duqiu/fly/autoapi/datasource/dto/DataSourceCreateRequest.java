package org.duqiu.fly.autoapi.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;

@Data
public class DataSourceCreateRequest {
    
    @NotBlank(message = "数据源名称不能为空")
    private String name;
    
    private String description;
    
    @NotNull(message = "数据库类型不能为空")
    private DataSourceType type;
    
    @NotBlank(message = "主机地址不能为空")
    private String host;
    
    @NotNull(message = "端口不能为空")
    @Min(value = 1, message = "端口必须大于0")
    @Max(value = 65535, message = "端口不能超过65535")
    private Integer port;
    
    private String database;
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @Min(value = 1, message = "连接池大小至少为1")
    @Max(value = 100, message = "连接池大小不能超过100")
    private Integer maxPoolSize = 10;
    
    @Min(value = 1000, message = "连接超时时间至少为1000ms")
    private Integer connectionTimeout = 30000;
    
    private String testQuery;
}