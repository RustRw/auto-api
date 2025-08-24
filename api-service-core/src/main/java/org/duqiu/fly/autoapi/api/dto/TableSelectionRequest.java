package org.duqiu.fly.autoapi.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 表选择请求DTO
 */
@Data
public class TableSelectionRequest {
    
    private String databaseName;
    private String schemaName;
    private String tableName;
    private String tableAlias;
    private Boolean isPrimary = false;
    private String joinCondition;
    private String joinType;
    private Integer sortOrder = 0;
    private List<String> selectedColumns;
}