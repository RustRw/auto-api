package org.duqiu.fly.autoapi.datasource.controller;

import jakarta.validation.Valid;
import org.duqiu.fly.autoapi.common.dto.PageResult;
import org.duqiu.fly.autoapi.common.dto.Result;
import org.duqiu.fly.autoapi.common.context.TenantContext;
import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponseV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceUpdateRequest;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.service.EnhancedDataSourceService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源管理REST控制器 - 提供完整的CRUD和元数据查询功能
 */
@RestController("coreDataSourceController")
@RequestMapping("/api/v2/datasources")
public class DataSourceController {
    
    private final EnhancedDataSourceService dataSourceService;
    
    public DataSourceController(EnhancedDataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }
    
    // ======================== CRUD操作 ========================
    
    /**
     * 创建数据源
     */
    @PostMapping
    public Result<DataSourceResponseV2> createDataSource(@Valid @RequestBody DataSourceCreateRequestV2 request) {
        try {
            Long userId = getCurrentUserId();
            Long tenantId = getCurrentTenantId();
            DataSourceResponseV2 response = dataSourceService.createDataSource(request, userId, tenantId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("创建数据源失败: " + e.getMessage());
        }
    }
    
    /**
     * 分页查询用户数据源
     */
    @GetMapping
    public Result<PageResult<DataSourceResponseV2>> getUserDataSources(
            Pageable pageable,
            @RequestParam(required = false) DataSourceType type,
            @RequestParam(required = false) String keyword) {
        try {
            Long tenantId = getCurrentTenantId();
            PageResult<DataSourceResponseV2> result = dataSourceService.getUserDataSources(tenantId, pageable, type, keyword);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取数据源列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取数据源详情
     */
    @GetMapping("/{id}")
    public Result<DataSourceResponseV2> getDataSource(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            DataSourceResponseV2 response = dataSourceService.getDataSourceById(id, userId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("获取数据源失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新数据源
     */
    @PutMapping("/{id}")
    public Result<DataSourceResponseV2> updateDataSource(
            @PathVariable Long id,
            @Valid @RequestBody DataSourceUpdateRequest request) {
        try {
            Long userId = getCurrentUserId();
            DataSourceResponseV2 response = dataSourceService.updateDataSource(id, request, userId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("更新数据源失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除数据源（软删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDataSource(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            dataSourceService.deleteDataSource(id, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除数据源失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除数据源
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteDataSources(@RequestBody List<Long> ids) {
        try {
            Long userId = getCurrentUserId();
            dataSourceService.batchDeleteDataSources(ids, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("批量删除数据源失败: " + e.getMessage());
        }
    }
    
    // ======================== 连接测试 ========================
    
    /**
     * 测试数据源连接
     */
    @PostMapping("/{id}/test")
    public Result<DataSourceResponseV2.ConnectionStatus> testConnection(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            DataSourceResponseV2.ConnectionStatus status = dataSourceService.testConnectionDetailed(id, userId);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("测试连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试数据源配置（不保存）
     */
    @PostMapping("/test-config")
    public Result<DataSourceResponseV2.ConnectionStatus> testDataSourceConfig(
            @Valid @RequestBody DataSourceCreateRequestV2 request) {
        try {
            Long userId = getCurrentUserId();
            DataSourceResponseV2.ConnectionStatus status = dataSourceService.testDataSourceConfig(request, userId);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("测试配置失败: " + e.getMessage());
        }
    }
    
    // ======================== 元数据查询 ========================
    
    /**
     * 获取数据源信息（数据源级别元数据）
     */
    @GetMapping("/{id}/metadata")
    public Result<DataSourceConnection.ConnectionInfo> getDataSourceMetadata(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            DataSourceConnection.ConnectionInfo metadata = dataSourceService.getDataSourceMetadata(id, userId);
            return Result.success(metadata);
        } catch (Exception e) {
            return Result.error("获取数据源信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取数据库列表（数据库级别元数据）
     */
    @GetMapping("/{id}/databases")
    public Result<List<String>> getDatabases(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            List<String> databases = dataSourceService.getDatabases(id, userId);
            return Result.success(databases);
        } catch (Exception e) {
            return Result.error("获取数据库列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表/集合列表（表级别元数据）
     */
    @GetMapping("/{id}/tables")
    public Result<List<DataSourceConnection.TableInfo>> getTables(
            @PathVariable Long id,
            @RequestParam(required = false) String database,
            @RequestParam(required = false) String schema) {
        try {
            Long userId = getCurrentUserId();
            List<DataSourceConnection.TableInfo> tables = dataSourceService.getTables(id, database, schema, userId);
            return Result.success(tables);
        } catch (Exception e) {
            return Result.error("获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表结构详细信息（字段和索引级别元数据）
     */
    @GetMapping("/{id}/tables/{tableName}/schema")
    public Result<DataSourceConnection.TableSchema> getTableSchema(
            @PathVariable Long id,
            @PathVariable String tableName,
            @RequestParam(required = false) String database,
            @RequestParam(required = false) String schema) {
        try {
            Long userId = getCurrentUserId();
            DataSourceConnection.TableSchema tableSchema = dataSourceService.getTableSchema(
                    id, tableName, database, schema, userId);
            return Result.success(tableSchema);
        } catch (Exception e) {
            return Result.error("获取表结构失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表的字段信息
     */
    @GetMapping("/{id}/tables/{tableName}/columns")
    public Result<List<DataSourceConnection.ColumnInfo>> getTableColumns(
            @PathVariable Long id,
            @PathVariable String tableName,
            @RequestParam(required = false) String database,
            @RequestParam(required = false) String schema) {
        try {
            Long userId = getCurrentUserId();
            List<DataSourceConnection.ColumnInfo> columns = dataSourceService.getTableColumns(
                    id, tableName, database, schema, userId);
            return Result.success(columns);
        } catch (Exception e) {
            return Result.error("获取表字段失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表的索引信息
     */
    @GetMapping("/{id}/tables/{tableName}/indexes")
    public Result<List<DataSourceConnection.IndexInfo>> getTableIndexes(
            @PathVariable Long id,
            @PathVariable String tableName,
            @RequestParam(required = false) String database,
            @RequestParam(required = false) String schema) {
        try {
            Long userId = getCurrentUserId();
            List<DataSourceConnection.IndexInfo> indexes = dataSourceService.getTableIndexes(
                    id, tableName, database, schema, userId);
            return Result.success(indexes);
        } catch (Exception e) {
            return Result.error("获取表索引失败: " + e.getMessage());
        }
    }
    
    // ======================== 查询执行 ========================
    
    /**
     * 执行查询
     */
    @PostMapping("/{id}/query")
    public Result<Map<String, Object>> executeQuery(
            @PathVariable Long id,
            @RequestBody Map<String, Object> queryRequest) {
        try {
            Long userId = getCurrentUserId();
            String query = (String) queryRequest.get("query");
            Map<String, Object> parameters = (Map<String, Object>) queryRequest.getOrDefault("parameters", Map.of());
            Integer limit = (Integer) queryRequest.getOrDefault("limit", 100);
            
            Map<String, Object> result = dataSourceService.executeQuery(id, query, parameters, limit, userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("执行查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证查询语句
     */
    @PostMapping("/{id}/query/validate")
    public Result<Map<String, Object>> validateQuery(
            @PathVariable Long id,
            @RequestBody Map<String, Object> queryRequest) {
        try {
            Long userId = getCurrentUserId();
            String query = (String) queryRequest.get("query");
            Map<String, Object> result = dataSourceService.validateQuery(id, query, userId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("验证查询失败: " + e.getMessage());
        }
    }
    
    // ======================== 系统信息 ========================
    
    /**
     * 获取支持的数据源类型
     */
    @GetMapping("/types")
    public Result<List<DataSourceType>> getSupportedDataSourceTypes() {
        try {
            List<DataSourceType> types = dataSourceService.getSupportedDataSourceTypes();
            return Result.success(types);
        } catch (Exception e) {
            return Result.error("获取数据源类型失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取数据源类型的依赖信息
     */
    @GetMapping("/types/{type}/dependency")
    public Result<UnifiedDataSourceFactory.DependencyInfo> getDataSourceTypeDependency(@PathVariable DataSourceType type) {
        try {
            UnifiedDataSourceFactory.DependencyInfo info = dataSourceService.getDependencyInfo(type);
            return Result.success(info);
        } catch (Exception e) {
            return Result.error("获取依赖信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查所有数据源类型的依赖状态
     */
    @GetMapping("/dependencies/check")
    public Result<Map<DataSourceType, Boolean>> checkAllDependencies() {
        try {
            Map<DataSourceType, Boolean> statusMap = dataSourceService.checkAllDependencies();
            return Result.success(statusMap);
        } catch (Exception e) {
            return Result.error("检查依赖状态失败: " + e.getMessage());
        }
    }
    
    // ======================== 私有方法 ========================
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            throw new RuntimeException("用户未认证");
        }
        return userId;
    }
    
    /**
     * 获取当前租户ID
     */
    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new RuntimeException("租户信息缺失");
        }
        return tenantId;
    }
}