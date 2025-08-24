package org.duqiu.fly.autoapi.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.duqiu.fly.autoapi.api.model.ApiService;

/**
 * API服务更新请求DTO
 */
@Data
public class ApiServiceUpdateRequest {
    
    @NotBlank(message = "API名称不能为空")
    private String name;
    
    private String description;
    
    @NotBlank(message = "API路径不能为空")
    private String path;
    
    private ApiService.HttpMethod method;
    
    @NotBlank(message = "SQL内容不能为空")
    private String sqlContent;
    
    private String requestParams;
    
    private String responseExample;
    
    private Boolean cacheEnabled;
    
    private Integer cacheDuration;
    
    private Integer rateLimit;
    
    /**
     * 更新说明
     */
    private String updateDescription;
}