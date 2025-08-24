package org.duqiu.fly.autoapi.api.repository;

import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API服务审计日志Repository
 */
@Repository
public interface ApiServiceAuditLogRepository extends JpaRepository<ApiServiceAuditLog, Long> {
    
    /**
     * 根据API服务ID查找操作日志，按时间倒序
     */
    Page<ApiServiceAuditLog> findByApiServiceIdOrderByCreatedAtDesc(Long apiServiceId, Pageable pageable);
    
    /**
     * 根据操作人查找日志
     */
    Page<ApiServiceAuditLog> findByCreatedByOrderByCreatedAtDesc(Long createdBy, Pageable pageable);
    
    /**
     * 根据操作类型查找日志
     */
    List<ApiServiceAuditLog> findByApiServiceIdAndOperationTypeOrderByCreatedAtDesc(
            Long apiServiceId, ApiServiceAuditLog.OperationType operationType);
    
    /**
     * 查找指定时间范围内的日志
     */
    @Query("SELECT log FROM ApiServiceAuditLog log WHERE log.apiServiceId = :apiServiceId " +
           "AND log.createdAt BETWEEN :startTime AND :endTime ORDER BY log.createdAt DESC")
    List<ApiServiceAuditLog> findByApiServiceIdAndTimeRange(
            @Param("apiServiceId") Long apiServiceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计操作次数
     */
    @Query("SELECT COUNT(log) FROM ApiServiceAuditLog log WHERE log.apiServiceId = :apiServiceId " +
           "AND log.operationType = :operationType AND log.operationResult = :result")
    long countOperations(@Param("apiServiceId") Long apiServiceId,
                        @Param("operationType") ApiServiceAuditLog.OperationType operationType,
                        @Param("result") ApiServiceAuditLog.OperationResult result);
    
    /**
     * 查找最近的操作记录
     */
    @Query("SELECT log FROM ApiServiceAuditLog log WHERE log.apiServiceId = :apiServiceId " +
           "ORDER BY log.createdAt DESC")
    List<ApiServiceAuditLog> findRecentOperations(@Param("apiServiceId") Long apiServiceId, Pageable pageable);
    
    /**
     * 删除指定API服务的所有日志
     */
    void deleteByApiServiceId(Long apiServiceId);
    
    /**
     * 删除指定时间之前的日志（用于日志清理）
     */
    @Query("DELETE FROM ApiServiceAuditLog log WHERE log.createdAt < :cutoffTime")
    void deleteLogsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * 统计API服务的日志数量
     */
    long countByApiServiceId(Long apiServiceId);
}