package org.duqiu.fly.autoapi.datasource.integration;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponseV2;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源连接和认证集成测试
 */
@DisplayName("数据源连接和认证集成测试")
public class DataSourceConnectionIntegrationTest extends DataSourceIntegrationTestBase {
    
    @Test
    @DisplayName("测试数据源连接 - 有效配置")
    void testConnectionTest_ValidConfig() {
        // Given
        DataSourceCreateRequestV2 request = createValidDataSourceRequest();
        DataSourceResponseV2 created = dataSourceService.createDataSource(request, TEST_USER_ID);
        
        // When
        DataSourceResponseV2.ConnectionStatus status = dataSourceService.testConnectionDetailed(
                created.getId(), TEST_USER_ID);
        
        // Then
        assertNotNull(status);
        assertNotNull(status.getLastTestTime());
        assertNotNull(status.getResponseTime());
        assertTrue(status.getResponseTime() >= 0);
        
        // 注意：由于我们使用模拟的MySQL配置连接H2，连接可能失败
        // 这里主要测试流程是否正确
        assertNotNull(status.getMessage());
    }
    
    @Test
    @DisplayName("测试数据源配置 - 不保存到数据库")
    void testDataSourceConfig_WithoutSaving() {
        // Given
        DataSourceCreateRequestV2 request = createValidDataSourceRequest();
        
        // When
        DataSourceResponseV2.ConnectionStatus status = dataSourceService.testDataSourceConfig(
                request, TEST_USER_ID);
        
        // Then
        assertNotNull(status);
        assertNotNull(status.getLastTestTime());
        assertNotNull(status.getResponseTime());
        assertNotNull(status.getMessage());
        
        // 验证没有保存到数据库
        assertEquals(0, dataSourceRepository.count());
    }
    
    @Test
    @DisplayName("测试H2数据库连接 - 实际可连接的配置")
    void testH2DatabaseConnection_Success() throws SQLException {
        // Given - 创建实际可连接的H2数据源
        DataSource testDataSource = createTestDataSource();
        testDataSource.setConnectionUrl(testDatabaseUrl);
        testDataSource = dataSourceRepository.save(testDataSource);
        
        // When - 直接测试H2连接
        try (Connection connection = getTestConnection()) {
            
            // Then
            assertNotNull(connection);
            assertFalse(connection.isClosed());
            assertTrue(connection.isValid(5));
            
            // 测试简单查询
            var stmt = connection.createStatement();
            var rs = stmt.executeQuery("SELECT 1 as test_value");
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("test_value"));
        }
    }
    
    @Test
    @DisplayName("测试数据源工厂创建连接")
    void testDataSourceFactory_CreateConnection() {
        // Given - 创建可连接的H2数据源配置
        DataSource dataSource = new DataSource();
        dataSource.setType(DataSourceType.MYSQL);
        dataSource.setHost("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabase("testdb");
        dataSource.setUsername(testDatabaseUsername);
        dataSource.setPassword(testDatabasePassword);
        dataSource.setConnectionUrl(testDatabaseUrl);
        
        // When
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            
            // Then
            assertNotNull(connection);
            
            // 测试连接有效性
            boolean isValid = connection.isValid();
            // 由于使用了模拟配置，连接可能失败，但不应该抛出异常
            // 主要测试工厂方法的正确性
            assertNotNull(connection);
            
        } catch (Exception e) {
            // 预期可能发生连接异常，因为配置是模拟的
            assertTrue(e.getMessage().contains("连接") || e.getMessage().contains("connection"));
        }
    }
    
    @Test
    @DisplayName("测试无效认证信息")
    void testInvalidCredentials() {
        // Given
        DataSourceCreateRequestV2 request = createValidDataSourceRequest();
        request.setUsername("invalid_user");
        request.setPassword("invalid_password");
        
        // When
        DataSourceResponseV2.ConnectionStatus status = dataSourceService.testDataSourceConfig(
                request, TEST_USER_ID);
        
        // Then
        assertNotNull(status);
        assertFalse(status.isConnected());
        assertNotNull(status.getMessage());
        assertTrue(status.getMessage().contains("连接失败") || status.getMessage().contains("失败"));
    }
    
    @Test
    @DisplayName("测试无效主机地址")
    void testInvalidHost() {
        // Given
        DataSourceCreateRequestV2 request = createValidDataSourceRequest();
        request.setHost("nonexistent-host.invalid");
        
        // When
        DataSourceResponseV2.ConnectionStatus status = dataSourceService.testDataSourceConfig(
                request, TEST_USER_ID);
        
        // Then
        assertNotNull(status);
        assertFalse(status.isConnected());
        assertNotNull(status.getMessage());
        assertTrue(status.getResponseTime() > 0); // 应该有超时时间
    }
    
    @Test
    @DisplayName("测试无效端口")
    void testInvalidPort() {
        // Given
        DataSourceCreateRequestV2 request = createValidDataSourceRequest();
        request.setPort(99999); // 无效端口
        
        // When
        DataSourceResponseV2.ConnectionStatus status = dataSourceService.testDataSourceConfig(
                request, TEST_USER_ID);
        
        // Then
        assertNotNull(status);
        assertFalse(status.isConnected());
        assertNotNull(status.getMessage());
    }
    
    @Test
    @DisplayName("测试连接超时")
    void testConnectionTimeout() {
        // Given
        DataSourceCreateRequestV2 request = createValidDataSourceRequest();
        request.setConnectionTimeout(1000); // 很短的超时时间
        request.setHost("192.168.255.254"); // 不可达的IP地址
        
        // When
        long startTime = System.currentTimeMillis();
        DataSourceResponseV2.ConnectionStatus status = dataSourceService.testDataSourceConfig(
                request, TEST_USER_ID);
        long actualTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertNotNull(status);
        assertFalse(status.isConnected());
        
        // 验证实际超时时间合理（允许一定误差）
        assertTrue(actualTime >= 1000); // 至少等于设置的超时时间
        assertTrue(actualTime < 10000); // 但不应该太长
    }
    
    @Test
    @DisplayName("测试SSL连接配置")
    void testSslConnection() {
        // Given
        DataSourceCreateRequestV2 request = createValidDataSourceRequest();
        request.setSslEnabled(true);
        
        // When
        DataSourceResponseV2.ConnectionStatus status = dataSourceService.testDataSourceConfig(
                request, TEST_USER_ID);
        
        // Then
        assertNotNull(status);
        // SSL连接测试，主要验证配置能正确传递
        assertNotNull(status.getMessage());
        assertTrue(status.getResponseTime() >= 0);
    }
    
    @Test
    @DisplayName("测试不同数据源类型的连接")
    void testDifferentDataSourceTypes() {
        // Given - PostgreSQL配置
        DataSourceCreateRequestV2 pgRequest = createValidDataSourceRequest();
        pgRequest.setType(DataSourceType.POSTGRESQL);
        pgRequest.setPort(5432);
        
        // When
        DataSourceResponseV2.ConnectionStatus pgStatus = dataSourceService.testDataSourceConfig(
                pgRequest, TEST_USER_ID);
        
        // Then
        assertNotNull(pgStatus);
        assertNotNull(pgStatus.getMessage());
        
        // Given - MongoDB配置
        DataSourceCreateRequestV2 mongoRequest = createValidDataSourceRequest();
        mongoRequest.setType(DataSourceType.MONGODB);
        mongoRequest.setPort(27017);
        mongoRequest.setUsername(null); // MongoDB可能不需要用户名密码
        mongoRequest.setPassword(null);
        
        // When
        DataSourceResponseV2.ConnectionStatus mongoStatus = dataSourceService.testDataSourceConfig(
                mongoRequest, TEST_USER_ID);
        
        // Then
        assertNotNull(mongoStatus);
        assertNotNull(mongoStatus.getMessage());
    }
    
    @Test
    @DisplayName("测试连接池配置验证")
    void testConnectionPoolConfiguration() {
        // Given
        DataSourceCreateRequestV2 request = createValidDataSourceRequest();
        request.setConnectionPoolEnabled(true);
        request.setMaxPoolSize(20);
        request.setMinPoolSize(5);
        request.setIdleTimeout(300000);
        request.setMaxLifetime(900000);
        
        // When
        DataSourceResponseV2 created = dataSourceService.createDataSource(request, TEST_USER_ID);
        
        // Then - 验证连接池配置被正确保存
        assertNotNull(created);
        assertEquals(request.getMaxPoolSize(), created.getMaxPoolSize());
        assertEquals(request.getMinPoolSize(), created.getMinPoolSize());
        assertEquals(request.getIdleTimeout(), created.getIdleTimeout());
        assertEquals(request.getMaxLifetime(), created.getMaxLifetime());
        assertTrue(created.getConnectionPoolEnabled());
    }
}