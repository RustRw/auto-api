package org.duqiu.fly.autoapi.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.duqiu.fly.autoapi.api.model.ApiService;

@Data
public class ApiServiceCreateRequest {
    
    @NotBlank(message = "API名称不能为空")
    private String name;
    
    private String description;
    
    @NotBlank(message = "API路径不能为空")
    private String path;
    
    @NotNull(message = "HTTP方法不能为空")
    private ApiService.HttpMethod method;
    
    @NotNull(message = "数据源ID不能为空")
    private Long dataSourceId;
    
    @NotBlank(message = "SQL内容不能为空")
    private String sqlContent;
    
    private String requestParams;
    
    private String responseExample;
    
    private Boolean cacheEnabled = false;
    
    private Integer cacheDuration = 300;
    
    private Integer rateLimit = 100;
}