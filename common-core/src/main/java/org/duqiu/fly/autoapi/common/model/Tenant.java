package org.duqiu.fly.autoapi.common.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户实体
 */
@Entity
@Table(name = "tenants")
@Data
@EqualsAndHashCode(callSuper = false)
public class Tenant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;
    
    @Column(unique = true, nullable = false, length = 100)
    private String tenantCode;
    
    @Column(nullable = false, length = 200)
    private String tenantName;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;
    
    @Column(name = "max_users")
    private Integer maxUsers = 10;
    
    @Column(name = "max_datasources")
    private Integer maxDataSources = 5;
    
    @Column(name = "max_api_services")
    private Integer maxApiServices = 20;
    
    @Column(name = "contact_email", length = 100)
    private String contactEmail;
    
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
    
    public enum TenantStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}