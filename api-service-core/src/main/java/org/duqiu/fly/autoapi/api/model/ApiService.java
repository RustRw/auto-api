package org.duqiu.fly.autoapi.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.duqiu.fly.autoapi.common.model.TenantAwareBaseEntity;

@Entity
@Table(name = "api_services")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiService extends TenantAwareBaseEntity {
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 200)
    private String description;
    
    @Column(nullable = false, length = 200)
    private String path;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethod method = HttpMethod.GET;
    
    @Column(name = "datasource_id", nullable = false)
    private Long dataSourceId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String sqlContent;
    
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;
    
    @Column(name = "response_example", columnDefinition = "TEXT")
    private String responseExample;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApiStatus status = ApiStatus.DRAFT;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "cache_enabled")
    private Boolean cacheEnabled = false;
    
    @Column(name = "cache_duration")
    private Integer cacheDuration = 300;
    
    @Column(name = "rate_limit")
    private Integer rateLimit = 100;
    
    public enum HttpMethod {
        GET, POST, PUT, DELETE
    }
}