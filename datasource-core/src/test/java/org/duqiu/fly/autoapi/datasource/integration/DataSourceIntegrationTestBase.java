package org.duqiu.fly.autoapi.datasource.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.duqiu.fly.autoapi.datasource.service.EnhancedDataSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据源集成测试基类
 */
@SpringBootTest(classes = org.duqiu.fly.autoapi.datasource.TestApplication.class)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = {"/schema.sql", "/data.sql"})
public abstract class DataSourceIntegrationTestBase {
    
    @Autowired
    protected DataSourceRepository dataSourceRepository;
    
    @Autowired
    protected UnifiedDataSourceFactory dataSourceFactory;
    
    @Autowired
    protected EnhancedDataSourceService dataSourceService;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected javax.sql.DataSource testDataSource;
    
    @Value("${test.database.h2.url}")
    protected String testDatabaseUrl;
    
    @Value("${test.database.h2.username}")
    protected String testDatabaseUsername;
    
    @Value("${test.database.h2.password}")
    protected String testDatabasePassword;
    
    @Value("${test.database.host}")
    protected String mockHost;
    
    @Value("${test.database.port}")
    protected Integer mockPort;
    
    @Value("${test.database.database}")
    protected String mockDatabase;
    
    @Value("${test.database.username}")
    protected String mockUsername;
    
    @Value("${test.database.password}")
    protected String mockPassword;
    
    protected static final Long TEST_USER_ID = 1L;
    
    @BeforeEach
    void setUp() {
        // 清理测试数据
        dataSourceRepository.deleteAll();
    }
    
    /**
     * 创建有效的数据源创建请求
     */
    protected DataSourceCreateRequestV2 createValidDataSourceRequest() {
        DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
        request.setName("MySQL Test DataSource");
        request.setDescription("Integration test MySQL datasource");
        request.setType(DataSourceType.MYSQL);
        request.setHost(mockHost);
        request.setPort(mockPort);
        request.setDatabase(mockDatabase);
        request.setUsername(mockUsername);
        request.setPassword(mockPassword);
        request.setMaxPoolSize(10);
        request.setMinPoolSize(1);
        request.setConnectionTimeout(30000);
        request.setIdleTimeout(600000);
        request.setMaxLifetime(1800000);
        request.setSslEnabled(false);
        request.setConnectionPoolEnabled(true);
        return request;
    }
    
    /**
     * 创建实际可连接的H2数据源请求（模拟MySQL）
     */
    protected DataSourceCreateRequestV2 createConnectableDataSourceRequest() {
        DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
        request.setName("H2 Mock MySQL DataSource");
        request.setDescription("H2 database that mocks MySQL for testing");
        request.setType(DataSourceType.MYSQL);
        // 使用H2的连接信息，但类型设为MySQL
        request.setHost("localhost");
        request.setPort(9092); // H2 TCP模式端口，这里用内存模式
        request.setDatabase("testdb");
        request.setUsername(testDatabaseUsername);
        request.setPassword(testDatabasePassword);
        request.setMaxPoolSize(5);
        request.setMinPoolSize(1);
        request.setConnectionTimeout(10000);
        request.setIdleTimeout(300000);
        request.setMaxLifetime(900000);
        request.setSslEnabled(false);
        request.setConnectionPoolEnabled(true);
        return request;
    }
    
    /**
     * 创建测试数据源实体
     */
    protected org.duqiu.fly.autoapi.datasource.model.DataSource createTestDataSource() {
        org.duqiu.fly.autoapi.datasource.model.DataSource dataSource = new org.duqiu.fly.autoapi.datasource.model.DataSource();
        dataSource.setName("Test MySQL DataSource");
        dataSource.setDescription("Test MySQL datasource for integration testing");
        dataSource.setType(DataSourceType.MYSQL);
        dataSource.setHost(mockHost);
        dataSource.setPort(mockPort);
        dataSource.setDatabase(mockDatabase);
        dataSource.setUsername(mockUsername);
        dataSource.setPassword(mockPassword);
        dataSource.setConnectionUrl(testDatabaseUrl);
        dataSource.setMaxPoolSize(10);
        dataSource.setMinPoolSize(1);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setSslEnabled(false);
        dataSource.setConnectionPoolEnabled(true);
        dataSource.setEnabled(true);
        dataSource.setCreatedBy(TEST_USER_ID);
        dataSource.setUpdatedBy(TEST_USER_ID);
        return dataSource;
    }
    
    /**
     * 获取测试数据库连接
     */
    protected Connection getTestConnection() throws SQLException {
        return testDataSource.getConnection();
    }
    
    /**
     * 验证数据源基本信息
     */
    protected void assertDataSourceBasicInfo(org.duqiu.fly.autoapi.datasource.model.DataSource dataSource, DataSourceCreateRequestV2 request) {
        assert dataSource != null;
        assert dataSource.getName().equals(request.getName());
        assert dataSource.getDescription().equals(request.getDescription());
        assert dataSource.getType().equals(request.getType());
        assert dataSource.getHost().equals(request.getHost());
        assert dataSource.getPort().equals(request.getPort());
        assert dataSource.getDatabase().equals(request.getDatabase());
        assert dataSource.getUsername().equals(request.getUsername());
        assert dataSource.getEnabled();
    }
}