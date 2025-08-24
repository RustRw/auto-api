package org.duqiu.fly.autoapi.auth.repository;

import org.duqiu.fly.autoapi.common.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    
    Optional<Tenant> findByTenantCode(String tenantCode);
    
    boolean existsByTenantCode(String tenantCode);
    
    Optional<Tenant> findByTenantCodeAndStatus(String tenantCode, Tenant.TenantStatus status);
}