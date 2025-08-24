package org.duqiu.fly.autoapi.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duqiu.fly.autoapi.ApiServiceTestApplication;
import org.duqiu.fly.autoapi.api.repository.ApiServiceAuditLogRepository;
import org.duqiu.fly.autoapi.api.repository.ApiServiceRepository;
import org.duqiu.fly.autoapi.api.repository.ApiServiceTableSelectionRepository;
import org.duqiu.fly.autoapi.api.repository.ApiServiceVersionRepository;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * API服务集成测试基类
 */
@SpringBootTest(classes = ApiServiceTestApplication.class)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = {"/test-schema.sql", "/test-data.sql"})
public abstract class ApiServiceIntegrationTestBase {
    
    @Autowired
    protected ApiServiceRepository apiServiceRepository;
    
    @Autowired
    protected ApiServiceVersionRepository versionRepository;
    
    @Autowired
    protected ApiServiceAuditLogRepository auditLogRepository;
    
    @Autowired
    protected ApiServiceTableSelectionRepository tableSelectionRepository;
    
    @Autowired
    protected DataSourceRepository dataSourceRepository;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected DataSource testDataSource;
    
    @Value("${test.datasource.host}")
    protected String mockHost;
    
    @Value("${test.datasource.port}")
    protected Integer mockPort;
    
    @Value("${test.datasource.database}")
    protected String mockDatabase;
    
    @Value("${test.datasource.username}")
    protected String mockUsername;
    
    @Value("${test.datasource.password}")
    protected String mockPassword;
    
    @Value("${test.datasource.h2.url}")
    protected String testDatabaseUrl;
    
    @Value("${test.datasource.h2.username}")
    protected String testDatabaseUsername;
    
    @Value("${test.datasource.h2.password}")
    protected String testDatabasePassword;
    
    protected static final Long TEST_USER_ID = 1L;
    protected static final Long OTHER_USER_ID = 2L;
    
    @BeforeEach
    void setUp() {
        // 清理可能存在的测试数据
        cleanupTestData();
    }
    
    /**
     * 清理测试数据
     */
    protected void cleanupTestData() {
        tableSelectionRepository.deleteAll();
        auditLogRepository.deleteAll();
        versionRepository.deleteAll();
        apiServiceRepository.deleteAll();
    }
    
    /**
     * 获取测试数据库连接
     */
    protected Connection getTestConnection() throws SQLException {
        return testDataSource.getConnection();
    }
    
    /**
     * 创建测试用的SQL查询
     */
    protected String createTestSqlQuery() {
        return "SELECT u.id, u.username, u.email, u.status " +
               "FROM test_users u " +
               "WHERE u.status = ${status} " +
               "ORDER BY u.created_at DESC";
    }
    
    /**
     * 创建复杂的JOIN查询
     */
    protected String createComplexJoinQuery() {
        return "SELECT u.username, p.name as product_name, o.quantity, o.total_price, o.status " +
               "FROM test_orders o " +
               "JOIN test_users u ON o.user_id = u.id " +
               "JOIN test_products p ON o.product_id = p.id " +
               "WHERE o.status = ${orderStatus} " +
               "AND u.status = ${userStatus} " +
               "ORDER BY o.order_date DESC " +
               "LIMIT ${limit}";
    }
    
    /**
     * 创建聚合查询
     */
    protected String createAggregateQuery() {
        return "SELECT u.username, COUNT(o.id) as order_count, SUM(o.total_price) as total_amount " +
               "FROM test_users u " +
               "LEFT JOIN test_orders o ON u.id = o.user_id " +
               "WHERE u.status = 'ACTIVE' " +
               "GROUP BY u.id, u.username " +
               "HAVING COUNT(o.id) > ${minOrders} " +
               "ORDER BY total_amount DESC";
    }
    
    /**
     * 验证审计日志记录
     */
    protected void verifyAuditLogCreated(Long apiServiceId, String operationType) {
        var logs = auditLogRepository.findByApiServiceIdOrderByCreatedAtDesc(apiServiceId, 
                org.springframework.data.domain.PageRequest.of(0, 10));
        
        assert !logs.isEmpty() : "应该有审计日志记录";
        
        var latestLog = logs.getContent().get(0);
        assert latestLog.getOperationType().toString().equals(operationType) : 
                "操作类型应该是 " + operationType;
        assert latestLog.getCreatedBy().equals(TEST_USER_ID) : "操作人应该是测试用户";
        assert latestLog.getOperationResult().toString().equals("SUCCESS") : "操作结果应该是成功";
    }
}