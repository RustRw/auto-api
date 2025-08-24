package org.duqiu.fly.autoapi.controller;

import jakarta.validation.Valid;
import org.duqiu.fly.autoapi.common.dto.Result;
import org.duqiu.fly.autoapi.common.context.TenantContext;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceUpdateRequest;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponseV2;
import org.duqiu.fly.autoapi.datasource.service.EnhancedDataSourceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据源管理控制器 - Web Gateway层
 */
@RestController
@RequestMapping("/api/datasources")
public class DataSourceController {
    
    private final EnhancedDataSourceService dataSourceService;
    
    public DataSourceController(EnhancedDataSourceService dataSourceService) {
        this.dataSourceService = dataSourceService;
    }
    
    @PostMapping
    public Result<DataSourceResponseV2> createDataSource(
            @Valid @RequestBody DataSourceCreateRequestV2 request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = TenantContext.getUserId();
            Long tenantId = TenantContext.getTenantId();
            if (userId == null || tenantId == null) {
                return Result.error("用户未认证或租户信息缺失");
            }
            DataSourceResponseV2 response = dataSourceService.createDataSource(request, userId, tenantId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("创建数据源失败: " + e.getMessage());
        }
    }
    
    @GetMapping
    public Result<List<DataSourceResponseV2>> getUserDataSources(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String host,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Result.error("租户信息缺失");
            }
            
            // 如果没有搜索条件且不需要分页，返回所有数据
            if (name == null && type == null && host == null && enabled == null && page == 0 && size >= 1000) {
                List<DataSourceResponseV2> dataSources = dataSourceService.getTenantDataSources(tenantId);
                return Result.success(dataSources);
            }
            
            // 否则调用支持搜索和分页的方法
            // TODO: 实现搜索和分页功能
            List<DataSourceResponseV2> dataSources = dataSourceService.getTenantDataSources(tenantId);
            
            // 临时的前端过滤实现（后续应该在数据库层面实现）
            System.out.println("搜索参数 - name: " + name + ", type: " + type + ", host: " + host + ", enabled: " + enabled);
            System.out.println("原始数据源数量: " + dataSources.size());
            
            List<DataSourceResponseV2> filteredDataSources = dataSources.stream()
                .filter(ds -> name == null || ds.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(ds -> type == null || ds.getType().toString().equals(type))
                .filter(ds -> host == null || ds.getHost().toLowerCase().contains(host.toLowerCase()))
                .filter(ds -> enabled == null || ds.getEnabled().equals(enabled))
                .toList();
            
            System.out.println("过滤后数据源数量: " + filteredDataSources.size());
            
            return Result.success(filteredDataSources);
        } catch (Exception e) {
            return Result.error("获取数据源列表失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public Result<DataSourceResponseV2> getDataSource(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            DataSourceResponseV2 response = dataSourceService.getDataSourceById(id, userId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("获取数据源失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{id}/test")
    public Result<DataSourceResponseV2.ConnectionStatus> testConnection(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            DataSourceResponseV2.ConnectionStatus status = dataSourceService.testConnectionDetailed(id, userId);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("测试连接失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/test-config")
    public Result<DataSourceResponseV2.ConnectionStatus> testDataSourceConfig(
            @Valid @RequestBody DataSourceCreateRequestV2 request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            DataSourceResponseV2.ConnectionStatus status = dataSourceService.testDataSourceConfig(request, userId);
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("测试配置失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public Result<DataSourceResponseV2> updateDataSource(
            @PathVariable Long id,
            @Valid @RequestBody DataSourceUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = TenantContext.getUserId();
            if (userId == null) {
                return Result.error("用户未认证");
            }
            DataSourceResponseV2 response = dataSourceService.updateDataSource(id, request, userId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("更新数据源失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteDataSource(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            dataSourceService.deleteDataSource(id, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除数据源失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取数据源的数据库列表
     */
    @GetMapping("/{id}/databases")
    public Result<List<String>> getDatabases(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            List<String> databases = dataSourceService.getDatabases(id, userId, search);
            return Result.success(databases);
        } catch (Exception e) {
            return Result.error("获取数据库列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取数据库的表列表
     */
    @GetMapping("/{id}/databases/{database}/tables")
    public Result<List<String>> getTables(
            @PathVariable Long id,
            @PathVariable String database,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            List<String> tables = dataSourceService.getTables(id, userId, database, search);
            return Result.success(tables);
        } catch (Exception e) {
            return Result.error("获取数据表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表结构
     */
    @GetMapping("/{id}/databases/{database}/tables/{table}/structure")
    public Result<List<Map<String, Object>>> getTableStructure(
            @PathVariable Long id,
            @PathVariable String database,
            @PathVariable String table,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            List<Map<String, Object>> structure = dataSourceService.getTableStructure(id, userId, database, table);
            return Result.success(structure);
        } catch (Exception e) {
            return Result.error("获取表结构失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表示例数据
     */
    @GetMapping("/{id}/databases/{database}/tables/{table}/sample-data")
    public Result<Map<String, Object>> getTableSampleData(
            @PathVariable Long id,
            @PathVariable String database,
            @PathVariable String table,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            Map<String, Object> sampleData = dataSourceService.getTableSampleData(id, userId, database, table, limit);
            return Result.success(sampleData);
        } catch (Exception e) {
            return Result.error("获取示例数据失败: " + e.getMessage());
        }
    }
}