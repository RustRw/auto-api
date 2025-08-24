package org.duqiu.fly.autoapi.datasource.repository;

import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.common.repository.TenantAwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends TenantAwareRepository<DataSource, Long> {
    
    List<DataSource> findByTenantIdAndEnabledTrue(Long tenantId);
    
    Page<DataSource> findByTenantIdAndEnabledTrue(Long tenantId, Pageable pageable);
    
    @Query("SELECT d FROM DataSource d WHERE d.tenantId = :tenantId AND d.enabled = true " +
           "AND (:type IS NULL OR d.type = :type) " +
           "AND (:keyword IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<DataSource> findByTenantIdAndFilters(@Param("tenantId") Long tenantId,
                                            @Param("type") DataSourceType type,
                                            @Param("keyword") String keyword,
                                            Pageable pageable);
    
    boolean existsByNameAndTenantId(String name, Long tenantId);
    
    Optional<DataSource> findByNameAndTenantId(String name, Long tenantId);
    
    // Methods needed for legacy tests
    boolean existsByNameAndCreatedBy(String name, Long createdBy);
    
    List<DataSource> findByCreatedByAndEnabledTrue(Long createdBy);
}