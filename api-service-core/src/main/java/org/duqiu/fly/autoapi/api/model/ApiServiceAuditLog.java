package org.duqiu.fly.autoapi.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.duqiu.fly.autoapi.common.model.TenantAwareBaseEntity;

import java.time.LocalDateTime;

/**
 * API服务操作审计日志实体
 */
@Entity
@Table(name = "api_service_audit_logs")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiServiceAuditLog extends TenantAwareBaseEntity {
    
    /**
     * 关联的API服务ID
     */
    @Column(name = "api_service_id", nullable = false)
    private Long apiServiceId;
    
    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType operationType;
    
    /**
     * 操作描述
     */
    @Column(nullable = false, length = 200)
    private String operationDescription;
    
    /**
     * 操作详情（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String operationDetails;
    
    /**
     * 操作前数据快照（JSON格式）
     */
    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;
    
    /**
     * 操作后数据快照（JSON格式）
     */
    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;
    
    /**
     * 操作结果
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationResult operationResult;
    
    /**
     * 错误信息（如果操作失败）
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 操作人IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * 操作持续时间（毫秒）
     */
    @Column(name = "duration_ms")
    private Long durationMs;
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        CREATE("创建"),
        UPDATE("更新"),
        DELETE("删除"),
        PUBLISH("发布"),
        UNPUBLISH("下线"),
        TEST("测试"),
        VERSION_COMPARE("版本对比");
        
        private final String description;
        
        OperationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 操作结果枚举
     */
    public enum OperationResult {
        SUCCESS("成功"),
        FAILED("失败"),
        PARTIAL_SUCCESS("部分成功");
        
        private final String description;
        
        OperationResult(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}