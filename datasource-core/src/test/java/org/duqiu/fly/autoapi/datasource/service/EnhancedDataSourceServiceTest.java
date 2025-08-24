package org.duqiu.fly.autoapi.datasource.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponseV2;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 增强数据源服务测试类
 */
@ExtendWith(MockitoExtension.class)
class EnhancedDataSourceServiceTest {
    
    @Mock
    private DataSourceRepository dataSourceRepository;
    
    @Mock
    private UnifiedDataSourceFactory dataSourceFactory;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private EnhancedDataSourceService dataSourceService;
    
    @BeforeEach
    void setUp() {
        dataSourceService = new EnhancedDataSourceService(
                dataSourceRepository, dataSourceFactory, objectMapper);
    }
    
    @Test
    void testCreateDataSource_Success() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        Long userId = 1L;
        
        DataSource savedDataSource = createDataSource();
        savedDataSource.setId(1L);
        
        when(dataSourceRepository.existsByNameAndCreatedBy(request.getName(), userId))
                .thenReturn(false);
        when(dataSourceFactory.validateConfiguration(any(DataSource.class)))
                .thenReturn(createValidationResult(true, null));
        when(dataSourceFactory.testConnection(any(DataSource.class)))
                .thenReturn(true);
        when(dataSourceFactory.buildConnectionUrl(any(DataSource.class)))
                .thenReturn("jdbc:mysql://localhost:3306/testdb");
        when(dataSourceRepository.save(any(DataSource.class)))
                .thenReturn(savedDataSource);
        when(dataSourceFactory.getDependencyInfo(any(DataSourceType.class)))
                .thenReturn(createDependencyInfo());
        when(dataSourceFactory.isDependencyAvailable(any(DataSourceType.class)))
                .thenReturn(true);
        
        // When
        DataSourceResponseV2 response = dataSourceService.createDataSource(request, userId);
        
        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getType(), response.getType());
        
        verify(dataSourceRepository).existsByNameAndCreatedBy(request.getName(), userId);
        verify(dataSourceFactory).validateConfiguration(any(DataSource.class));
        verify(dataSourceFactory).testConnection(any(DataSource.class));
        verify(dataSourceRepository).save(any(DataSource.class));
    }
    
    @Test
    void testCreateDataSource_DuplicateName() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        Long userId = 1L;
        
        when(dataSourceRepository.existsByNameAndCreatedBy(request.getName(), userId))
                .thenReturn(true);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> dataSourceService.createDataSource(request, userId));
        assertEquals("数据源名称已存在", exception.getMessage());
        
        verify(dataSourceRepository).existsByNameAndCreatedBy(request.getName(), userId);
        verify(dataSourceFactory, never()).validateConfiguration(any());
    }
    
    @Test
    void testCreateDataSource_ConnectionFailed() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        Long userId = 1L;
        
        when(dataSourceRepository.existsByNameAndCreatedBy(request.getName(), userId))
                .thenReturn(false);
        when(dataSourceFactory.validateConfiguration(any(DataSource.class)))
                .thenReturn(createValidationResult(true, null));
        when(dataSourceFactory.testConnection(any(DataSource.class)))
                .thenReturn(false);
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dataSourceService.createDataSource(request, userId));
        assertEquals("无法连接到数据源，请检查配置", exception.getMessage());
        
        verify(dataSourceFactory).testConnection(any(DataSource.class));
        verify(dataSourceRepository, never()).save(any());
    }
    
    @Test
    void testGetUserDataSources() {
        // Given
        Long userId = 1L;
        List<DataSource> dataSources = List.of(createDataSource());
        
        when(dataSourceRepository.findByCreatedByAndEnabledTrue(userId))
                .thenReturn(dataSources);
        when(dataSourceFactory.getDependencyInfo(any(DataSourceType.class)))
                .thenReturn(createDependencyInfo());
        when(dataSourceFactory.isDependencyAvailable(any(DataSourceType.class)))
                .thenReturn(true);
        
        // When
        List<DataSourceResponseV2> responses = dataSourceService.getUserDataSources(userId);
        
        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(dataSources.get(0).getName(), responses.get(0).getName());
        
        verify(dataSourceRepository).findByCreatedByAndEnabledTrue(userId);
    }
    
    @Test
    void testGetDataSourceById_Success() {
        // Given
        Long id = 1L;
        Long userId = 1L;
        DataSource dataSource = createDataSource();
        dataSource.setId(id);
        dataSource.setCreatedBy(userId);
        
        when(dataSourceRepository.findById(id))
                .thenReturn(Optional.of(dataSource));
        when(dataSourceFactory.getDependencyInfo(any(DataSourceType.class)))
                .thenReturn(createDependencyInfo());
        when(dataSourceFactory.isDependencyAvailable(any(DataSourceType.class)))
                .thenReturn(true);
        
        // When
        DataSourceResponseV2 response = dataSourceService.getDataSourceById(id, userId);
        
        // Then
        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals(dataSource.getName(), response.getName());
        
        verify(dataSourceRepository).findById(id);
    }
    
    @Test
    void testGetDataSourceById_NotFound() {
        // Given
        Long id = 1L;
        Long userId = 1L;
        
        when(dataSourceRepository.findById(id))
                .thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dataSourceService.getDataSourceById(id, userId));
        assertEquals("数据源不存在", exception.getMessage());
        
        verify(dataSourceRepository).findById(id);
    }
    
    @Test
    void testGetDataSourceById_AccessDenied() {
        // Given
        Long id = 1L;
        Long userId = 1L;
        Long ownerId = 2L;
        
        DataSource dataSource = createDataSource();
        dataSource.setId(id);
        dataSource.setCreatedBy(ownerId);
        
        when(dataSourceRepository.findById(id))
                .thenReturn(Optional.of(dataSource));
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dataSourceService.getDataSourceById(id, userId));
        assertEquals("无权访问该数据源", exception.getMessage());
        
        verify(dataSourceRepository).findById(id);
    }
    
    @Test
    void testDeleteDataSource_Success() {
        // Given
        Long id = 1L;
        Long userId = 1L;
        
        DataSource dataSource = createDataSource();
        dataSource.setId(id);
        dataSource.setCreatedBy(userId);
        dataSource.setEnabled(true);
        
        when(dataSourceRepository.findById(id))
                .thenReturn(Optional.of(dataSource));
        when(dataSourceRepository.save(any(DataSource.class)))
                .thenReturn(dataSource);
        
        // When
        dataSourceService.deleteDataSource(id, userId);
        
        // Then
        verify(dataSourceRepository).findById(id);
        verify(dataSourceRepository).save(argThat(ds -> 
                !ds.getEnabled() && ds.getUpdatedBy().equals(userId)));
    }
    
    @Test
    void testGetSupportedDataSourceTypes() {
        // When
        List<DataSourceType> types = dataSourceService.getSupportedDataSourceTypes();
        
        // Then
        assertNotNull(types);
        assertTrue(types.size() > 0);
        assertTrue(types.contains(DataSourceType.MYSQL));
    }
    
    // Helper methods
    private DataSourceCreateRequestV2 createValidRequest() {
        DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
        request.setName("Test DataSource");
        request.setDescription("Test Description");
        request.setType(DataSourceType.MYSQL);
        request.setHost("localhost");
        request.setPort(3306);
        request.setDatabase("testdb");
        request.setUsername("testuser");
        request.setPassword("testpass");
        request.setMaxPoolSize(10);
        request.setMinPoolSize(1);
        request.setConnectionTimeout(30000);
        request.setIdleTimeout(600000);
        request.setMaxLifetime(1800000);
        request.setSslEnabled(false);
        request.setConnectionPoolEnabled(true);
        return request;
    }
    
    private DataSource createDataSource() {
        DataSource dataSource = new DataSource();
        dataSource.setName("Test DataSource");
        dataSource.setDescription("Test Description");
        dataSource.setType(DataSourceType.MYSQL);
        dataSource.setHost("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabase("testdb");
        dataSource.setUsername("testuser");
        dataSource.setPassword("testpass");
        dataSource.setMaxPoolSize(10);
        dataSource.setMinPoolSize(1);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setSslEnabled(false);
        dataSource.setConnectionPoolEnabled(true);
        dataSource.setEnabled(true);
        dataSource.setCreatedBy(1L);
        dataSource.setUpdatedBy(1L);
        return dataSource;
    }
    
    private UnifiedDataSourceFactory.ValidationResult createValidationResult(boolean valid, String errorMessage) {
        return new UnifiedDataSourceFactory.ValidationResult() {
            @Override
            public boolean isValid() {
                return valid;
            }
            
            @Override
            public String getErrorMessage() {
                return errorMessage;
            }
            
            @Override
            public String getRecommendation() {
                return "Test recommendation";
            }
        };
    }
    
    private UnifiedDataSourceFactory.DependencyInfo createDependencyInfo() {
        return new UnifiedDataSourceFactory.DependencyInfo(
                "mysql:mysql-connector-java",
                List.of("8.0.33", "8.0.32"),
                "8.0.33"
        );
    }
}