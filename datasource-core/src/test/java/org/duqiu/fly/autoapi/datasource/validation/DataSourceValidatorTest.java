package org.duqiu.fly.autoapi.datasource.validation;

import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.validation.DataSourceValidator.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据源验证器测试类
 */
class DataSourceValidatorTest {
    
    private DataSourceValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new DataSourceValidator();
    }
    
    @Test
    void testValidateCreateRequest_ValidRequest() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void testValidateCreateRequest_EmptyName() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setName("");
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("数据源名称不能为空"));
    }
    
    @Test
    void testValidateCreateRequest_LongName() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setName("a".repeat(101)); // 超过100字符
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("数据源名称长度不能超过100个字符"));
    }
    
    @Test
    void testValidateCreateRequest_NullType() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setType(null);
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("数据源类型不能为空"));
    }
    
    @Test
    void testValidateCreateRequest_InvalidHost() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setHost("invalid-host-!@#");
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("主机地址格式无效"));
    }
    
    @Test
    void testValidateCreateRequest_InvalidPort() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setPort(70000); // 超过65535
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("端口号必须在1-65535之间"));
    }
    
    @Test
    void testValidateCreateRequest_InvalidConnectionPoolConfig() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setMaxPoolSize(5);
        request.setMinPoolSize(10); // 最小连接数大于最大连接数
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("最小连接数不能大于最大连接数"));
    }
    
    @Test
    void testValidateCreateRequest_JdbcWithoutCredentials() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setType(DataSourceType.MYSQL);
        request.setUsername(null);
        request.setPassword(null);
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("JDBC数据源必须提供用户名"));
        assertTrue(result.getErrors().contains("JDBC数据源必须提供密码"));
    }
    
    @Test
    void testValidateQuery_ValidSqlQuery() {
        // Given
        String query = "SELECT * FROM users WHERE id = 1";
        DataSourceType type = DataSourceType.MYSQL;
        
        // When
        ValidationResult result = validator.validateQuery(query, type);
        
        // Then
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }
    
    @Test
    void testValidateQuery_EmptyQuery() {
        // Given
        String query = "";
        DataSourceType type = DataSourceType.MYSQL;
        
        // When
        ValidationResult result = validator.validateQuery(query, type);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("查询语句不能为空"));
    }
    
    @Test
    void testValidateQuery_SqlInjection() {
        // Given
        String query = "SELECT * FROM users; DROP TABLE users;";
        DataSourceType type = DataSourceType.MYSQL;
        
        // When
        ValidationResult result = validator.validateQuery(query, type);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("查询语句包含潜在的安全风险"));
    }
    
    @Test
    void testValidateQuery_NonSelectQuery() {
        // Given
        String query = "UPDATE users SET name = 'test' WHERE id = 1";
        DataSourceType type = DataSourceType.MYSQL;
        
        // When
        ValidationResult result = validator.validateQuery(query, type);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("只支持SELECT查询"));
    }
    
    @Test
    void testValidateQuery_MongoDeleteOperation() {
        // Given
        String query = "db.users.drop()";
        DataSourceType type = DataSourceType.MONGODB;
        
        // When
        ValidationResult result = validator.validateQuery(query, type);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("不支持删除操作"));
    }
    
    @Test
    void testValidateQuery_ElasticsearchInvalidMethod() {
        // Given
        String query = "DELETE /users";
        DataSourceType type = DataSourceType.ELASTICSEARCH;
        
        // When
        ValidationResult result = validator.validateQuery(query, type);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("只支持GET和POST查询"));
    }
    
    @Test
    void testValidateHost_ValidIpAddress() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setHost("192.168.1.100");
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertTrue(result.isValid());
    }
    
    @Test
    void testValidateHost_ValidDomainName() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setHost("example.com");
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertTrue(result.isValid());
    }
    
    @Test
    void testValidateDatabase_ValidName() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setDatabase("test_database_123");
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertTrue(result.isValid());
    }
    
    @Test
    void testValidateDatabase_InvalidName() {
        // Given
        DataSourceCreateRequestV2 request = createValidRequest();
        request.setDatabase("123invalid"); // 不能以数字开头
        
        // When
        ValidationResult result = validator.validateCreateRequest(request);
        
        // Then
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("数据库名称格式无效"));
    }
    
    // Helper method
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
}