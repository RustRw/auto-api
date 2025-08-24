package org.duqiu.fly.autoapi.common.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * 支持多租户的基础实体类
 */
@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class TenantAwareBaseEntity {
    
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
    
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
}