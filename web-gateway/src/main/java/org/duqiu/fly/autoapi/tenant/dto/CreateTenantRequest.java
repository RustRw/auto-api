package org.duqiu.fly.autoapi.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTenantRequest {
    
    @NotBlank(message = "租户代码不能为空")
    @Size(max = 100, message = "租户代码不能超过100个字符")
    private String tenantCode;
    
    @NotBlank(message = "租户名称不能为空")
    @Size(max = 200, message = "租户名称不能超过200个字符")
    private String tenantName;
    
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
    
    @Email(message = "邮箱格式不正确")
    private String contactEmail;
    
    private String contactPhone;
    
    private Integer maxUsers = 10;
    
    private Integer maxDataSources = 5;
    
    private Integer maxApiServices = 20;
}