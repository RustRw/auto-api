package org.duqiu.fly.autoapi.api.dto;

import lombok.Data;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;

import java.time.LocalDateTime;

@Data
public class ApiServiceResponse {
    private Long id;
    private String name;
    private String description;
    private String path;
    private ApiService.HttpMethod method;
    private Long dataSourceId;
    private String dataSourceName; // 数据源名称
    private String sqlContent;
    private String requestParams;
    private String responseExample;
    private ApiStatus status;
    private Boolean enabled;
    private Boolean cacheEnabled;
    private Integer cacheDuration;
    private Integer rateLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private String createdByName; // 创建者姓名
    private String updatedByName; // 更新者姓名
    
    // 版本信息
    private String currentVersion;
    private Integer versionCount;
    private LocalDateTime lastPublishedAt;
}