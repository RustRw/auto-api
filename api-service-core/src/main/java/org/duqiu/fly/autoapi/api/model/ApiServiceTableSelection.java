package org.duqiu.fly.autoapi.api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.duqiu.fly.autoapi.common.model.TenantAwareBaseEntity;

/**
 * API服务关联的数据表选择实体
 */
@Entity
@Table(name = "api_service_table_selections")
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiServiceTableSelection extends TenantAwareBaseEntity {
    
    /**
     * 关联的API服务ID
     */
    @Column(name = "api_service_id", nullable = false)
    private Long apiServiceId;
    
    /**
     * 数据库名称
     */
    @Column(name = "database_name", length = 64)
    private String databaseName;
    
    /**
     * 模式名称
     */
    @Column(name = "schema_name", length = 64)
    private String schemaName;
    
    /**
     * 表名称
     */
    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;
    
    /**
     * 表别名（在SQL中使用）
     */
    @Column(name = "table_alias", length = 32)
    private String tableAlias;
    
    /**
     * 表类型（TABLE, VIEW等）
     */
    @Column(name = "table_type", length = 20)
    private String tableType;
    
    /**
     * 选择的字段列表（JSON格式）
     */
    @Column(name = "selected_columns", columnDefinition = "TEXT")
    private String selectedColumns;
    
    /**
     * 是否为主表
     */
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;
    
    /**
     * 关联条件（对于非主表）
     */
    @Column(name = "join_condition", columnDefinition = "TEXT")
    private String joinCondition;
    
    /**
     * 关联类型（INNER, LEFT, RIGHT等）
     */
    @Column(name = "join_type", length = 10)
    private String joinType;
    
    /**
     * 排序序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}