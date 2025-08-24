package org.duqiu.fly.autoapi.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duqiu.fly.autoapi.api.dto.TableSelectionRequest;
import org.duqiu.fly.autoapi.api.model.ApiServiceTableSelection;
import org.duqiu.fly.autoapi.api.repository.ApiServiceTableSelectionRepository;
import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.core.SchemaAwareConnection;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表选择管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TableSelectionService {
    
    private final ApiServiceTableSelectionRepository tableSelectionRepository;
    private final DataSourceRepository dataSourceRepository;
    private final UnifiedDataSourceFactory dataSourceFactory;
    private final ObjectMapper objectMapper;
    
    /**
     * 保存API服务的表选择配置
     */
    @Transactional
    public void saveTableSelections(Long apiServiceId, List<TableSelectionRequest> selections, Long userId) {
        // 删除现有配置
        tableSelectionRepository.deleteByApiServiceId(apiServiceId);
        
        // 保存新配置
        for (int i = 0; i < selections.size(); i++) {
            TableSelectionRequest request = selections.get(i);
            ApiServiceTableSelection selection = new ApiServiceTableSelection();
            
            selection.setApiServiceId(apiServiceId);
            selection.setDatabaseName(request.getDatabaseName());
            selection.setSchemaName(request.getSchemaName());
            selection.setTableName(request.getTableName());
            selection.setTableAlias(request.getTableAlias());
            selection.setIsPrimary(request.getIsPrimary());
            selection.setJoinCondition(request.getJoinCondition());
            selection.setJoinType(request.getJoinType());
            selection.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : i);
            selection.setCreatedBy(userId);
            selection.setUpdatedBy(userId);
            
            // 序列化选择的字段
            if (request.getSelectedColumns() != null && !request.getSelectedColumns().isEmpty()) {
                try {
                    selection.setSelectedColumns(objectMapper.writeValueAsString(request.getSelectedColumns()));
                } catch (Exception e) {
                    log.warn("序列化选择字段失败", e);
                }
            }
            
            tableSelectionRepository.save(selection);
        }
    }
    
    /**
     * 获取API服务的表选择配置
     */
    public List<TableSelectionRequest> getTableSelections(Long apiServiceId) {
        List<ApiServiceTableSelection> selections = 
                tableSelectionRepository.findByApiServiceIdOrderBySortOrder(apiServiceId);
        
        return selections.stream()
                .map(this::convertToRequest)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取数据源的表列表
     */
    public List<Map<String, Object>> getDataSourceTables(Long dataSourceId, String database, String schema, Long userId) {
        try {
            DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                    .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
            
            // 验证权限
            if (!dataSource.getCreatedBy().equals(userId)) {
                throw new IllegalArgumentException("无权访问该数据源");
            }
            
            try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
                List<DataSourceConnection.TableInfo> tables;
                
                if (connection instanceof SchemaAwareConnection && (database != null || schema != null)) {
                    tables = ((SchemaAwareConnection) connection).getTables(database, schema);
                } else {
                    tables = connection.getTables();
                }
                
                return tables.stream()
                        .map(table -> {
                            Map<String, Object> tableInfo = new HashMap<>();
                            tableInfo.put("name", table.getName());
                            tableInfo.put("type", table.getType());
                            tableInfo.put("comment", table.getComment());
                            return tableInfo;
                        })
                        .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            log.error("获取数据源表列表失败", e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表的字段列表
     */
    public List<Map<String, Object>> getTableColumns(Long dataSourceId, String tableName, 
                                                   String database, String schema, Long userId) {
        try {
            DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                    .orElseThrow(() -> new IllegalArgumentException("数据源不存在"));
            
            // 验证权限
            if (!dataSource.getCreatedBy().equals(userId)) {
                throw new IllegalArgumentException("无权访问该数据源");
            }
            
            try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
                DataSourceConnection.TableSchema tableSchema;
                
                if (connection instanceof SchemaAwareConnection && (database != null || schema != null)) {
                    tableSchema = ((SchemaAwareConnection) connection).getTableSchema(tableName, database, schema);
                } else {
                    tableSchema = connection.getTableSchema(tableName);
                }
                
                return tableSchema.getColumns().stream()
                        .map(column -> {
                            Map<String, Object> columnInfo = new HashMap<>();
                            columnInfo.put("name", column.getName());
                            columnInfo.put("type", column.getType());
                            columnInfo.put("nullable", column.isNullable());
                            columnInfo.put("comment", column.getComment());
                            columnInfo.put("defaultValue", column.getDefaultValue());
                            columnInfo.put("isPrimaryKey", false);
                            return columnInfo;
                        })
                        .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            log.error("获取表字段列表失败", e);
            throw new RuntimeException("获取字段列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成SQL语句模板
     */
    public String generateSqlTemplate(Long apiServiceId) {
        List<ApiServiceTableSelection> selections = 
                tableSelectionRepository.findByApiServiceIdOrderBySortOrder(apiServiceId);
        
        if (selections.isEmpty()) {
            return "SELECT * FROM your_table";
        }
        
        StringBuilder sqlBuilder = new StringBuilder();
        
        // 构建SELECT子句
        sqlBuilder.append("SELECT ");
        
        // 添加字段
        boolean firstColumn = true;
        for (ApiServiceTableSelection selection : selections) {
            List<String> columns = parseSelectedColumns(selection.getSelectedColumns());
            String tableAlias = selection.getTableAlias() != null ? 
                              selection.getTableAlias() : selection.getTableName();
            
            for (String column : columns) {
                if (!firstColumn) {
                    sqlBuilder.append(",\n       ");
                }
                sqlBuilder.append(tableAlias).append(".").append(column);
                firstColumn = false;
            }
        }
        
        // 构建FROM子句
        sqlBuilder.append("\nFROM ");
        
        // 主表
        ApiServiceTableSelection primaryTable = selections.stream()
                .filter(s -> s.getIsPrimary())
                .findFirst()
                .orElse(selections.get(0));
        
        sqlBuilder.append(getFullTableName(primaryTable));
        if (primaryTable.getTableAlias() != null) {
            sqlBuilder.append(" AS ").append(primaryTable.getTableAlias());
        }
        
        // 关联表
        for (ApiServiceTableSelection selection : selections) {
            if (!selection.getIsPrimary() && selection != primaryTable) {
                sqlBuilder.append("\n").append(selection.getJoinType()).append(" JOIN ");
                sqlBuilder.append(getFullTableName(selection));
                if (selection.getTableAlias() != null) {
                    sqlBuilder.append(" AS ").append(selection.getTableAlias());
                }
                if (selection.getJoinCondition() != null) {
                    sqlBuilder.append(" ON ").append(selection.getJoinCondition());
                }
            }
        }
        
        // 添加WHERE子句模板
        sqlBuilder.append("\nWHERE 1=1");
        sqlBuilder.append("\n  -- 在此处添加查询条件，可使用参数：${paramName}");
        
        return sqlBuilder.toString();
    }
    
    // ===== 私有方法 =====
    
    private TableSelectionRequest convertToRequest(ApiServiceTableSelection selection) {
        TableSelectionRequest request = new TableSelectionRequest();
        request.setDatabaseName(selection.getDatabaseName());
        request.setSchemaName(selection.getSchemaName());
        request.setTableName(selection.getTableName());
        request.setTableAlias(selection.getTableAlias());
        request.setIsPrimary(selection.getIsPrimary());
        request.setJoinCondition(selection.getJoinCondition());
        request.setJoinType(selection.getJoinType());
        request.setSortOrder(selection.getSortOrder());
        
        // 反序列化选择的字段
        if (selection.getSelectedColumns() != null) {
            try {
                List<String> columns = objectMapper.readValue(
                        selection.getSelectedColumns(), new TypeReference<List<String>>() {});
                request.setSelectedColumns(columns);
            } catch (Exception e) {
                log.warn("反序列化选择字段失败", e);
            }
        }
        
        return request;
    }
    
    private List<String> parseSelectedColumns(String selectedColumns) {
        if (selectedColumns == null || selectedColumns.trim().isEmpty()) {
            return List.of("*");
        }
        
        try {
            return objectMapper.readValue(selectedColumns, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析选择字段失败", e);
            return List.of("*");
        }
    }
    
    private String getFullTableName(ApiServiceTableSelection selection) {
        StringBuilder nameBuilder = new StringBuilder();
        
        if (selection.getDatabaseName() != null && !selection.getDatabaseName().trim().isEmpty()) {
            nameBuilder.append(selection.getDatabaseName()).append(".");
        }
        
        if (selection.getSchemaName() != null && !selection.getSchemaName().trim().isEmpty()) {
            nameBuilder.append(selection.getSchemaName()).append(".");
        }
        
        nameBuilder.append(selection.getTableName());
        
        return nameBuilder.toString();
    }
}