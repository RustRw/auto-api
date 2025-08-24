package org.duqiu.fly.autoapi.tenant.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TenantInfoResponse {
    
    private Long tenantId;
    
    private String tenantCode;
    
    private String tenantName;
    
    private String description;
    
    private String status;
    
    private Integer maxUsers;
    
    private Integer maxDataSources;
    
    private Integer maxApiServices;
    
    private String contactEmail;
    
    private String contactPhone;
    
    private LocalDateTime createdAt;
    
    private Long currentUserCount;
    
    private Long currentDataSourceCount;
    
    private Long currentApiServiceCount;
}