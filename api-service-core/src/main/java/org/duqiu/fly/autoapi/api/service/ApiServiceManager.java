package org.duqiu.fly.autoapi.api.service;

import org.duqiu.fly.autoapi.api.dto.ApiServiceCreateRequest;
import org.duqiu.fly.autoapi.api.dto.ApiServiceResponse;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.repository.ApiServiceRepository;
import org.duqiu.fly.autoapi.common.dto.PageResult;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.duqiu.fly.autoapi.common.context.TenantContext;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ApiServiceManager {
    
    private final ApiServiceRepository apiServiceRepository;
    private final DataSourceRepository dataSourceRepository;
    
    public ApiServiceManager(ApiServiceRepository apiServiceRepository,
                           DataSourceRepository dataSourceRepository) {
        this.apiServiceRepository = apiServiceRepository;
        this.dataSourceRepository = dataSourceRepository;
    }
    
    public ApiServiceResponse createApiService(ApiServiceCreateRequest request, Long userId) {
        // 验证数据源是否存在且属于当前用户
        DataSource dataSource = dataSourceRepository.findById(request.getDataSourceId())
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权使用该数据源");
        }
        
        // 检查路径和方法是否已存在
        if (apiServiceRepository.existsByPathAndMethodAndCreatedBy(
                request.getPath(), request.getMethod(), userId)) {
            throw new RuntimeException("该路径和方法的API已存在");
        }
        
        ApiService apiService = new ApiService();
        apiService.setName(request.getName());
        apiService.setDescription(request.getDescription());
        apiService.setPath(request.getPath());
        apiService.setMethod(request.getMethod());
        apiService.setDataSourceId(request.getDataSourceId());
        apiService.setSqlContent(request.getSqlContent());
        apiService.setRequestParams(request.getRequestParams());
        apiService.setCacheEnabled(request.getCacheEnabled());
        apiService.setCacheDuration(request.getCacheDuration());
        apiService.setRateLimit(request.getRateLimit());
        apiService.setCreatedBy(userId);
        apiService.setUpdatedBy(userId);
        
        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        apiService.setCreatedAt(now);
        apiService.setUpdatedAt(now);
        
        // Set tenant ID from context
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            // Default tenant ID for single-tenant mode
            tenantId = 1L;
        }
        apiService.setTenantId(tenantId);
        
        ApiService saved = apiServiceRepository.save(apiService);
        return convertToResponse(saved);
    }
    
    public PageResult<ApiServiceResponse> getUserApiServices(Long userId, Pageable pageable) {
        Page<ApiService> page = apiServiceRepository.findByCreatedByAndEnabledTrue(userId, pageable);
        List<ApiServiceResponse> responses = page.getContent().stream()
                .map(this::convertToResponse)
                .toList();
        
        PageResult<ApiServiceResponse> result = new PageResult<>();
        result.setContent(responses);
        result.setPageNumber(page.getNumber());
        result.setPageSize(page.getSize());
        result.setTotalElements(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        result.setFirst(page.isFirst());
        result.setLast(page.isLast());
        return result;
    }
    
    public ApiServiceResponse getApiServiceById(Long id, Long userId) {
        ApiService apiService = apiServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API服务不存在"));
        
        if (!apiService.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该API服务");
        }
        
        return convertToResponse(apiService);
    }
    
    public ApiServiceResponse updateApiService(Long id, ApiServiceCreateRequest request, Long userId) {
        ApiService apiService = apiServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API服务不存在"));
        
        if (!apiService.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权修改该API服务");
        }
        
        // 更新API服务字段
        apiService.setName(request.getName());
        apiService.setDescription(request.getDescription());
        apiService.setPath(request.getPath());
        apiService.setMethod(request.getMethod());
        apiService.setDataSourceId(request.getDataSourceId());
        apiService.setSqlContent(request.getSqlContent());
        apiService.setRequestParams(request.getRequestParams());
        apiService.setResponseExample(request.getResponseExample());
        apiService.setCacheEnabled(request.getCacheEnabled());
        apiService.setCacheDuration(request.getCacheDuration());
        apiService.setRateLimit(request.getRateLimit());
        apiService.setUpdatedBy(userId);
        apiService.setUpdatedAt(LocalDateTime.now());
        
        // Ensure tenant ID is set if not already present
        if (apiService.getTenantId() == null) {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                tenantId = 1L; // Default tenant ID for single-tenant mode
            }
            apiService.setTenantId(tenantId);
        }
        
        ApiService saved = apiServiceRepository.save(apiService);
        return convertToResponse(saved);
    }
    
    public ApiServiceResponse updateApiServiceStatus(Long id, ApiStatus status, Long userId) {
        ApiService apiService = apiServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API服务不存在"));
        
        if (!apiService.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权修改该API服务");
        }
        
        apiService.setStatus(status);
        apiService.setUpdatedBy(userId);
        apiService.setUpdatedAt(LocalDateTime.now());
        
        ApiService saved = apiServiceRepository.save(apiService);
        return convertToResponse(saved);
    }
    
    public void deleteApiService(Long id, Long userId) {
        ApiService apiService = apiServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API服务不存在"));
        
        if (!apiService.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权删除该API服务");
        }
        
        apiService.setEnabled(false);
        apiService.setUpdatedBy(userId);
        apiService.setUpdatedAt(LocalDateTime.now());
        apiServiceRepository.save(apiService);
    }
    
    public Map<String, Object> executeApi(String path, ApiService.HttpMethod method, 
                                         Map<String, Object> params) {
        ApiService apiService = apiServiceRepository.findByPathAndMethodAndStatus(
                path, method, ApiStatus.PUBLISHED)
                .orElseThrow(() -> new RuntimeException("API不存在或未发布"));
        
        DataSource dataSource = dataSourceRepository.findById(apiService.getDataSourceId())
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        return executeSql(dataSource, apiService.getSqlContent(), params);
    }
    
    private Map<String, Object> executeSql(DataSource dataSource, String sql, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try (Connection connection = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                dataSource.getPassword())) {
            
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                // 简化处理，实际应该解析参数并设置到PreparedStatement中
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    
                    int columnCount = resultSet.getMetaData().getColumnCount();
                    while (resultSet.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = resultSet.getMetaData().getColumnName(i);
                            Object value = resultSet.getObject(i);
                            row.put(columnName, value);
                        }
                        rows.add(row);
                    }
                    
                    result.put("data", rows);
                    result.put("count", rows.size());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("执行SQL失败: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    private ApiServiceResponse convertToResponse(ApiService apiService) {
        ApiServiceResponse response = new ApiServiceResponse();
        response.setId(apiService.getId());
        response.setName(apiService.getName());
        response.setDescription(apiService.getDescription());
        response.setPath(apiService.getPath());
        response.setMethod(apiService.getMethod());
        response.setDataSourceId(apiService.getDataSourceId());
        
        // 设置数据源名称
        if (apiService.getDataSourceId() != null) {
            dataSourceRepository.findById(apiService.getDataSourceId())
                .ifPresent(dataSource -> response.setDataSourceName(dataSource.getName()));
        }
        
        response.setSqlContent(apiService.getSqlContent());
        response.setRequestParams(apiService.getRequestParams());
        response.setResponseExample(apiService.getResponseExample());
        response.setStatus(apiService.getStatus());
        response.setEnabled(apiService.getEnabled());
        response.setCacheEnabled(apiService.getCacheEnabled());
        response.setCacheDuration(apiService.getCacheDuration());
        response.setRateLimit(apiService.getRateLimit());
        response.setCreatedAt(apiService.getCreatedAt());
        response.setUpdatedAt(apiService.getUpdatedAt());
        return response;
    }
}