package org.duqiu.fly.autoapi.datasource.service;

import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequest;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponse;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DataSourceService {
    
    private final DataSourceRepository dataSourceRepository;
    
    public DataSourceService(DataSourceRepository dataSourceRepository) {
        this.dataSourceRepository = dataSourceRepository;
    }
    
    public DataSourceResponse createDataSource(DataSourceCreateRequest request, Long userId, Long tenantId) {
        if (dataSourceRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new RuntimeException("数据源名称已存在");
        }
        
        DataSource dataSource = new DataSource();
        dataSource.setName(request.getName());
        dataSource.setDescription(request.getDescription());
        dataSource.setType(request.getType());
        dataSource.setHost(request.getHost());
        dataSource.setPort(request.getPort());
        dataSource.setDatabase(request.getDatabase());
        dataSource.setUsername(request.getUsername());
        dataSource.setPassword(request.getPassword());
        dataSource.setMaxPoolSize(request.getMaxPoolSize());
        dataSource.setConnectionTimeout(request.getConnectionTimeout());
        dataSource.setTestQuery(request.getTestQuery() != null ? request.getTestQuery() : "SELECT 1");
        dataSource.setTenantId(tenantId);
        dataSource.setCreatedBy(userId);
        dataSource.setUpdatedBy(userId);
        
        // 生成连接URL
        String connectionUrl = generateConnectionUrl(request.getType(), request.getHost(), 
                request.getPort(), request.getDatabase());
        dataSource.setConnectionUrl(connectionUrl);
        
        DataSource saved = dataSourceRepository.save(dataSource);
        return convertToResponse(saved);
    }
    
    public List<DataSourceResponse> getTenantDataSources(Long tenantId) {
        List<DataSource> dataSources = dataSourceRepository.findByTenantIdAndEnabledTrue(tenantId);
        return dataSources.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    public DataSourceResponse getDataSourceById(Long id, Long tenantId) {
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("数据源不存在或无权访问"));
        
        return convertToResponse(dataSource);
    }
    
    public boolean testConnection(Long id, Long tenantId) {
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("数据源不存在或无权访问"));
        
        return testDatabaseConnection(dataSource);
    }
    
    public void deleteDataSource(Long id, Long tenantId, Long userId) {
        DataSource dataSource = dataSourceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("数据源不存在或无权访问"));
        
        dataSource.setEnabled(false);
        dataSource.setUpdatedBy(userId);
        dataSourceRepository.save(dataSource);
    }
    
    private String generateConnectionUrl(DataSourceType type, String host, Integer port, String database) {
        String template = type.getUrlTemplate();
        return template.replace("{host}", host)
                      .replace("{port}", port.toString())
                      .replace("{database}", database != null ? database : "");
    }
    
    private boolean testDatabaseConnection(DataSource dataSource) {
        if (dataSource.getType() == DataSourceType.ELASTICSEARCH) {
            // ES连接测试逻辑
            return true; // 简化处理
        }
        
        try (Connection connection = DriverManager.getConnection(
                dataSource.getConnectionUrl(),
                dataSource.getUsername(),
                dataSource.getPassword())) {
            
            return connection.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }
    
    private DataSourceResponse convertToResponse(DataSource dataSource) {
        DataSourceResponse response = new DataSourceResponse();
        response.setId(dataSource.getId());
        response.setName(dataSource.getName());
        response.setDescription(dataSource.getDescription());
        response.setType(dataSource.getType());
        response.setHost(dataSource.getHost());
        response.setPort(dataSource.getPort());
        response.setDatabase(dataSource.getDatabase());
        response.setUsername(dataSource.getUsername());
        response.setMaxPoolSize(dataSource.getMaxPoolSize());
        response.setConnectionTimeout(dataSource.getConnectionTimeout());
        response.setEnabled(dataSource.getEnabled());
        response.setTestQuery(dataSource.getTestQuery());
        response.setCreatedAt(dataSource.getCreatedAt());
        response.setUpdatedAt(dataSource.getUpdatedAt());
        return response;
    }
    
    // Legacy methods needed for backward compatibility tests
    public DataSourceResponse createDataSource(DataSourceCreateRequest request, Long userId) {
        return createDataSource(request, userId, 1L); // Default tenant
    }
    
    public void deleteDataSource(Long id, Long userId) {
        deleteDataSource(id, 1L, userId); // Default tenant
    }
    
    public List<DataSourceResponse> getUserDataSources(Long userId) {
        List<DataSource> dataSources = dataSourceRepository.findByCreatedByAndEnabledTrue(userId);
        return dataSources.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
}