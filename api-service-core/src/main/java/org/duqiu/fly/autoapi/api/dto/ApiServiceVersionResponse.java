package org.duqiu.fly.autoapi.api.dto;

import lombok.Data;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;

import java.time.LocalDateTime;

/**
 * API服务版本响应DTO
 */
@Data
public class ApiServiceVersionResponse {
    
    private Long id;
    private Long apiServiceId;
    private String version;
    private String versionDescription;
    private String name;
    private String description;
    private String path;
    private ApiService.HttpMethod method;
    private Long dataSourceId;
    private String sqlContent;
    private String requestParams;
    private String responseExample;
    private ApiStatus status;
    private Boolean isActive;
    private LocalDateTime publishedAt;
    private LocalDateTime unpublishedAt;
    private Boolean cacheEnabled;
    private Integer cacheDuration;
    private Integer rateLimit;
    private LocalDateTime createdAt;
    private Long createdBy;
    private String createdByName; // 创建者姓名
}