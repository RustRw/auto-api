package org.duqiu.fly.autoapi.common.repository;

import org.duqiu.fly.autoapi.common.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * 支持多租户的基础Repository接口
 */
@NoRepositoryBean
public interface TenantAwareRepository<T extends TenantAwareBaseEntity, ID> extends JpaRepository<T, ID> {
    
    List<T> findByTenantId(Long tenantId);
    
    Page<T> findByTenantId(Long tenantId, Pageable pageable);
    
    Optional<T> findByIdAndTenantId(ID id, Long tenantId);
    
    boolean existsByIdAndTenantId(ID id, Long tenantId);
    
    void deleteByIdAndTenantId(ID id, Long tenantId);
}