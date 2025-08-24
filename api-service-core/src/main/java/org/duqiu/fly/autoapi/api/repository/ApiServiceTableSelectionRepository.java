package org.duqiu.fly.autoapi.api.repository;

import org.duqiu.fly.autoapi.api.model.ApiServiceTableSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * API服务表选择Repository
 */
@Repository
public interface ApiServiceTableSelectionRepository extends JpaRepository<ApiServiceTableSelection, Long> {
    
    /**
     * 根据API服务ID查找所有表选择，按排序字段排序
     */
    List<ApiServiceTableSelection> findByApiServiceIdOrderBySortOrder(Long apiServiceId);
    
    /**
     * 查找主表
     */
    @Query("SELECT t FROM ApiServiceTableSelection t WHERE t.apiServiceId = :apiServiceId AND t.isPrimary = true")
    ApiServiceTableSelection findPrimaryTableByApiServiceId(@Param("apiServiceId") Long apiServiceId);
    
    /**
     * 查找非主表（关联表）
     */
    @Query("SELECT t FROM ApiServiceTableSelection t WHERE t.apiServiceId = :apiServiceId AND t.isPrimary = false ORDER BY t.sortOrder")
    List<ApiServiceTableSelection> findJoinTablesByApiServiceId(@Param("apiServiceId") Long apiServiceId);
    
    /**
     * 根据表名查找
     */
    List<ApiServiceTableSelection> findByApiServiceIdAndTableName(Long apiServiceId, String tableName);
    
    /**
     * 删除API服务的所有表选择
     */
    void deleteByApiServiceId(Long apiServiceId);
    
    /**
     * 统计API服务的表选择数量
     */
    int countByApiServiceId(Long apiServiceId);
}