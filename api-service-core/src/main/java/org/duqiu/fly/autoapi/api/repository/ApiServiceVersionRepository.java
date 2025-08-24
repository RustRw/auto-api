package org.duqiu.fly.autoapi.api.repository;

import org.duqiu.fly.autoapi.api.model.ApiServiceVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * API服务版本Repository
 */
@Repository
public interface ApiServiceVersionRepository extends JpaRepository<ApiServiceVersion, Long> {
    
    /**
     * 根据API服务ID查找所有版本，按创建时间倒序
     */
    List<ApiServiceVersion> findByApiServiceIdOrderByCreatedAtDesc(Long apiServiceId);
    
    /**
     * 分页查询API服务的版本
     */
    Page<ApiServiceVersion> findByApiServiceIdOrderByCreatedAtDesc(Long apiServiceId, Pageable pageable);
    
    /**
     * 根据API服务ID和版本号查找特定版本
     */
    Optional<ApiServiceVersion> findByApiServiceIdAndVersion(Long apiServiceId, String version);
    
    /**
     * 查找API服务的当前激活版本
     */
    @Query("SELECT v FROM ApiServiceVersion v WHERE v.apiServiceId = :apiServiceId AND v.isActive = true")
    Optional<ApiServiceVersion> findActiveVersionByApiServiceId(@Param("apiServiceId") Long apiServiceId);
    
    /**
     * 设置指定版本为激活状态，同时将其他版本设为非激活
     */
    @Modifying
    @Query("UPDATE ApiServiceVersion v SET v.isActive = CASE WHEN v.id = :versionId THEN true ELSE false END WHERE v.apiServiceId = :apiServiceId")
    void setActiveVersion(@Param("apiServiceId") Long apiServiceId, @Param("versionId") Long versionId);
    
    /**
     * 统计API服务的版本数量
     */
    long countByApiServiceId(Long apiServiceId);
    
    /**
     * 查找最新的版本号
     */
    @Query("SELECT MAX(v.version) FROM ApiServiceVersion v WHERE v.apiServiceId = :apiServiceId")
    Optional<String> findLatestVersionByApiServiceId(@Param("apiServiceId") Long apiServiceId);
    
    /**
     * 根据创建者查找版本
     */
    List<ApiServiceVersion> findByApiServiceIdAndCreatedByOrderByCreatedAtDesc(Long apiServiceId, Long createdBy);
    
    /**
     * 删除API服务的所有版本
     */
    void deleteByApiServiceId(Long apiServiceId);
}