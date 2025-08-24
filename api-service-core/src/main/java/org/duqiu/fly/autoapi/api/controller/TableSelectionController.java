package org.duqiu.fly.autoapi.api.controller;

import lombok.RequiredArgsConstructor;
import org.duqiu.fly.autoapi.api.dto.TableSelectionRequest;
import org.duqiu.fly.autoapi.api.service.TableSelectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表选择管理控制器
 */
@RestController
@RequestMapping("/api/v1/table-selection")
@RequiredArgsConstructor
public class TableSelectionController {
    
    private final TableSelectionService tableSelectionService;
    
    /**
     * 保存API服务的表选择配置
     */
    @PostMapping("/{apiServiceId}")
    public ResponseEntity<Void> saveTableSelections(
            @PathVariable Long apiServiceId,
            @RequestBody List<TableSelectionRequest> selections,
            @RequestHeader("X-User-Id") Long userId) {
        tableSelectionService.saveTableSelections(apiServiceId, selections, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取API服务的表选择配置
     */
    @GetMapping("/{apiServiceId}")
    public ResponseEntity<List<TableSelectionRequest>> getTableSelections(
            @PathVariable Long apiServiceId) {
        List<TableSelectionRequest> selections = tableSelectionService.getTableSelections(apiServiceId);
        return ResponseEntity.ok(selections);
    }
    
    /**
     * 获取数据源的表列表
     */
    @GetMapping("/datasource/{dataSourceId}/tables")
    public ResponseEntity<List<Map<String, Object>>> getDataSourceTables(
            @PathVariable Long dataSourceId,
            @RequestParam(required = false) String database,
            @RequestParam(required = false) String schema,
            @RequestHeader("X-User-Id") Long userId) {
        List<Map<String, Object>> tables = 
                tableSelectionService.getDataSourceTables(dataSourceId, database, schema, userId);
        return ResponseEntity.ok(tables);
    }
    
    /**
     * 获取表的字段列表
     */
    @GetMapping("/datasource/{dataSourceId}/tables/{tableName}/columns")
    public ResponseEntity<List<Map<String, Object>>> getTableColumns(
            @PathVariable Long dataSourceId,
            @PathVariable String tableName,
            @RequestParam(required = false) String database,
            @RequestParam(required = false) String schema,
            @RequestHeader("X-User-Id") Long userId) {
        List<Map<String, Object>> columns = 
                tableSelectionService.getTableColumns(dataSourceId, tableName, database, schema, userId);
        return ResponseEntity.ok(columns);
    }
    
    /**
     * 生成SQL语句模板
     */
    @GetMapping("/{apiServiceId}/sql-template")
    public ResponseEntity<Map<String, String>> generateSqlTemplate(
            @PathVariable Long apiServiceId) {
        String sqlTemplate = tableSelectionService.generateSqlTemplate(apiServiceId);
        return ResponseEntity.ok(Map.of("sqlTemplate", sqlTemplate));
    }
}