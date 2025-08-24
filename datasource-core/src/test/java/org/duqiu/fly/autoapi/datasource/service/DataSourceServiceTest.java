package org.duqiu.fly.autoapi.datasource.service;

import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequest;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponse;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceUpdateRequest;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.exception.DataSourceException;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DataSourceService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock
    private DataSourceRepository dataSourceRepository;

    private DataSourceService dataSourceService;

    @BeforeEach
    void setUp() {
        dataSourceService = new DataSourceService(dataSourceRepository);
    }

    @Test
    void testCreateDataSource_Success() {
        // Given
        DataSourceCreateRequest request = createValidCreateRequest();
        Long userId = 1L;
        
        DataSource savedDataSource = createDataSource();
        savedDataSource.setId(1L);
        
        when(dataSourceRepository.existsByNameAndCreatedBy(request.getName(), userId))
                .thenReturn(false);
        when(dataSourceRepository.save(any(DataSource.class)))
                .thenReturn(savedDataSource);

        // When
        DataSourceResponse response = dataSourceService.createDataSource(request, userId);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getType(), response.getType());
        
        verify(dataSourceRepository).existsByNameAndCreatedBy(request.getName(), userId);
        verify(dataSourceRepository).save(any(DataSource.class));
    }

    @Test
    void testCreateDataSource_DuplicateName() {
        // Given
        DataSourceCreateRequest request = createValidCreateRequest();
        Long userId = 1L;
        
        when(dataSourceRepository.existsByNameAndCreatedBy(request.getName(), userId))
                .thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dataSourceService.createDataSource(request, userId));
        assertEquals("数据源名称已存在", exception.getMessage());
        
        verify(dataSourceRepository).existsByNameAndCreatedBy(request.getName(), userId);
        verify(dataSourceRepository, never()).save(any());
    }

    @Test
    void testCreateDataSource_NullRequest() {
        // When & Then
        assertThrows(NullPointerException.class,
                () -> dataSourceService.createDataSource(null, 1L));
        
        verify(dataSourceRepository, never()).save(any());
    }

    @Test
    void testGetDataSourceById_Success() {
        // Given
        Long id = 1L;
        Long userId = 1L;
        DataSource dataSource = createDataSource();
        dataSource.setId(id);
        dataSource.setCreatedBy(userId);
        
        when(dataSourceRepository.findById(id)).thenReturn(Optional.of(dataSource));

        // When
        DataSourceResponse response = dataSourceService.getDataSourceById(id, userId);

        // Then
        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals(dataSource.getName(), response.getName());
        assertEquals(dataSource.getType(), response.getType());
        
        verify(dataSourceRepository).findById(id);
    }

    @Test
    void testGetDataSourceById_NotFound() {
        // Given
        Long id = 1L;
        Long userId = 1L;
        
        when(dataSourceRepository.findById(id)).thenReturn(Optional.empty());

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
        
        when(dataSourceRepository.findById(id)).thenReturn(Optional.of(dataSource));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> dataSourceService.getDataSourceById(id, userId));
        assertEquals("无权访问该数据源", exception.getMessage());
        
        verify(dataSourceRepository).findById(id);
    }

    // updateDataSource method doesn't exist in actual implementation, removing this test

    @Test
    void testDeleteDataSource_Success() {
        // Given
        Long id = 1L;
        Long userId = 1L;
        
        DataSource dataSource = createDataSource();
        dataSource.setId(id);
        dataSource.setCreatedBy(userId);
        dataSource.setEnabled(true);
        
        when(dataSourceRepository.findById(id)).thenReturn(Optional.of(dataSource));
        when(dataSourceRepository.save(any(DataSource.class))).thenReturn(dataSource);

        // When
        dataSourceService.deleteDataSource(id, userId);

        // Then
        verify(dataSourceRepository).findById(id);
        verify(dataSourceRepository).save(argThat(ds -> 
                !ds.getEnabled() && ds.getUpdatedBy().equals(userId)));
    }

    @Test
    void testGetUserDataSources() {
        // Given
        Long userId = 1L;
        List<DataSource> dataSources = Arrays.asList(
                createDataSource("DataSource 1"),
                createDataSource("DataSource 2")
        );
        
        when(dataSourceRepository.findByCreatedByAndEnabledTrue(userId))
                .thenReturn(dataSources);

        // When
        List<DataSourceResponse> responses = dataSourceService.getUserDataSources(userId);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("DataSource 1", responses.get(0).getName());
        assertEquals("DataSource 2", responses.get(1).getName());
        
        verify(dataSourceRepository).findByCreatedByAndEnabledTrue(userId);
    }

    // getDataSourcesByType method doesn't exist in actual implementation, removing this test

    @Test
    void testTestConnection_Success() {
        // Given
        Long id = 1L;
        Long userId = 1L;
        
        DataSource dataSource = createDataSource();
        dataSource.setId(id);
        dataSource.setCreatedBy(userId);
        dataSource.setType(DataSourceType.ELASTICSEARCH); // Use ES to avoid actual DB connection
        
        when(dataSourceRepository.findById(id)).thenReturn(Optional.of(dataSource));

        // When
        boolean result = dataSourceService.testConnection(id, userId);

        // Then
        assertTrue(result); // ES test returns true in simplified implementation
        verify(dataSourceRepository).findById(id);
    }

    // Helper methods
    private DataSourceCreateRequest createValidCreateRequest() {
        DataSourceCreateRequest request = new DataSourceCreateRequest();
        request.setName("Test DataSource");
        request.setDescription("Test Description");
        request.setType(DataSourceType.MYSQL);
        request.setHost("localhost");
        request.setPort(3306);
        request.setDatabase("testdb");
        request.setUsername("testuser");
        request.setPassword("testpass");
        return request;
    }

    // DataSourceUpdateRequest not used since updateDataSource method doesn't exist

    private DataSource createDataSource() {
        return createDataSource("Test DataSource");
    }

    private DataSource createDataSource(String name) {
        DataSource dataSource = new DataSource();
        dataSource.setName(name);
        dataSource.setDescription("Test Description");
        dataSource.setType(DataSourceType.MYSQL);
        dataSource.setHost("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabase("testdb");
        dataSource.setUsername("testuser");
        dataSource.setPassword("testpass");
        dataSource.setEnabled(true);
        dataSource.setCreatedBy(1L);
        dataSource.setUpdatedBy(1L);
        return dataSource;
    }
}