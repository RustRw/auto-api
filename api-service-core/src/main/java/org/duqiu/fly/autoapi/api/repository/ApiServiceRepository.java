package org.duqiu.fly.autoapi.api.repository;

import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiServiceRepository extends JpaRepository<ApiService, Long> {
    
    /**
     * 分页查询用户的启用API服务
     */
    Page<ApiService> findByCreatedByAndEnabledTrue(Long userId, Pageable pageable);
    
    /**
     * 根据用户和状态查询API服务
     */
    List<ApiService> findByCreatedByAndStatus(Long userId, ApiStatus status);
    
    /**
     * 根据路径、方法和状态查找API服务
     */
    Optional<ApiService> findByPathAndMethodAndStatus(String path, ApiService.HttpMethod method, ApiStatus status);
    
    /**
     * 检查路径和方法是否已被用户使用
     */
    boolean existsByPathAndMethodAndCreatedBy(String path, ApiService.HttpMethod method, Long userId);
    
    /**
     * 根据ID和创建者查找API服务
     */
    Optional<ApiService> findByIdAndCreatedBy(Long id, Long createdBy);
    
    /**
     * 检查名称是否已被用户使用
     */
    boolean existsByNameAndCreatedBy(String name, Long createdBy);
    
    /**
     * 根据创建者查找所有API服务
     */
    List<ApiService> findByCreatedByOrderByCreatedAtDesc(Long createdBy);
    
    /**
     * 根据创建者分页查找API服务
     */
    Page<ApiService> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);

    /**
     * 根据状态分页查找API服务
     */
    Page<ApiService> findByStatus(ApiStatus status, Pageable pageable);
    
    /**
     * 检查名称是否已被租户使用
     */
    boolean existsByNameAndTenantId(String name, Long tenantId);
}