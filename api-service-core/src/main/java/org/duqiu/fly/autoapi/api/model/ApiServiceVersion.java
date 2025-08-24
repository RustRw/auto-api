package org.duqiu.fly.autoapi.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.duqiu.fly.autoapi.common.model.TenantAwareBaseEntity;

import java.time.LocalDateTime;

/**
 * API服务版本实体 - 记录每次发布的版本信息
 */
@Entity
@Table(name = "api_service_versions")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiServiceVersion extends TenantAwareBaseEntity {
    
    /**
     * 关联的API服务ID
     */
    @Column(name = "api_service_id", nullable = false)
    private Long apiServiceId;
    
    /**
     * 版本号
     */
    @Column(nullable = false, length = 50)
    private String version;
    
    /**
     * 版本描述
     */
    @Column(length = 500)
    private String versionDescription;
    
    /**
     * API名称（快照）
     */
    @Column(nullable = false, length = 100)
    private String name;
    
    /**
     * API描述（快照）
     */
    @Column(length = 200)
    private String description;
    
    /**
     * API路径（快照）
     */
    @Column(nullable = false, length = 200)
    private String path;
    
    /**
     * HTTP方法（快照）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiService.HttpMethod method;
    
    /**
     * 数据源ID（快照）
     */
    @Column(name = "datasource_id", nullable = false)
    private Long dataSourceId;
    
    /**
     * SQL内容（快照）
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String sqlContent;
    
    /**
     * 请求参数配置（快照）
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;
    
    /**
     * 响应示例（快照）
     */
    @Column(name = "response_example", columnDefinition = "TEXT")
    private String responseExample;
    
    /**
     * 版本状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiStatus status;
    
    /**
     * 是否为当前激活版本
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;
    
    /**
     * 发布时间
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    /**
     * 下线时间
     */
    @Column(name = "unpublished_at")
    private LocalDateTime unpublishedAt;
    
    /**
     * 缓存配置（快照）
     */
    @Column(name = "cache_enabled")
    private Boolean cacheEnabled = false;
    
    @Column(name = "cache_duration")
    private Integer cacheDuration = 300;
    
    /**
     * 限流配置（快照）
     */
    @Column(name = "rate_limit")
    private Integer rateLimit = 100;
}