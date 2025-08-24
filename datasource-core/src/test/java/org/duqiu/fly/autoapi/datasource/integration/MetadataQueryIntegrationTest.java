package org.duqiu.fly.autoapi.datasource.integration;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponseV2;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.metadata.MetadataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 元数据查询集成测试
 */
@DisplayName("元数据查询集成测试")
public class MetadataQueryIntegrationTest extends DataSourceIntegrationTestBase {
    
    @Autowired
    private MetadataService metadataService;
    
    private DataSource testDataSource;
    
    @BeforeEach
    void setUpMetadataTest() throws SQLException {
        // 创建测试数据源
        testDataSource = createTestDataSource();
        testDataSource.setConnectionUrl(testDatabaseUrl);
        testDataSource = dataSourceRepository.save(testDataSource);
        
        // 确保测试数据已加载
        ensureTestDataExists();
    }
    
    private void ensureTestDataExists() throws SQLException {
        try (Connection conn = getTestConnection()) {
            Statement stmt = conn.createStatement();
            
            // 检查测试表是否存在，如果不存在则创建
            try {
                stmt.executeQuery("SELECT COUNT(*) FROM test_users");
            } catch (SQLException e) {
                // 表不存在，执行创建脚本
                executeSchemaScript(stmt);
                executeDataScript(stmt);
            }
        }
    }
    
    private void executeSchemaScript(Statement stmt) throws SQLException {
        // 简化的建表语句
        stmt.execute("DROP TABLE IF EXISTS test_orders");
        stmt.execute("DROP TABLE IF EXISTS test_products"); 
        stmt.execute("DROP TABLE IF EXISTS test_users");
        
        stmt.execute("""
                CREATE TABLE test_users (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    email VARCHAR(100) NOT NULL,
                    full_name VARCHAR(100),
                    age INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    is_active BOOLEAN DEFAULT TRUE
                )
                """);
                
        stmt.execute("""
                CREATE TABLE test_products (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL,
                    description TEXT,
                    price DECIMAL(10,2) NOT NULL,
                    category VARCHAR(50),
                    stock_quantity INT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
                
        stmt.execute("""
                CREATE TABLE test_orders (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    quantity INT NOT NULL,
                    total_amount DECIMAL(10,2) NOT NULL,
                    order_status VARCHAR(20) DEFAULT 'PENDING',
                    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
                
        // 创建索引
        stmt.execute("CREATE INDEX idx_users_username ON test_users(username)");
        stmt.execute("CREATE INDEX idx_users_email ON test_users(email)");
        stmt.execute("CREATE INDEX idx_orders_user_id ON test_orders(user_id)");
    }
    
    private void executeDataScript(Statement stmt) throws SQLException {
        // 插入测试数据
        stmt.execute("""
                INSERT INTO test_users (username, email, full_name, age, is_active) VALUES
                ('john_doe', 'john@example.com', 'John Doe', 30, true),
                ('jane_smith', 'jane@example.com', 'Jane Smith', 25, true),
                ('bob_wilson', 'bob@example.com', 'Bob Wilson', 35, false)
                """);
                
        stmt.execute("""
                INSERT INTO test_products (product_name, description, price, category, stock_quantity) VALUES
                ('Laptop Pro', 'High-performance laptop', 1999.99, 'Electronics', 50),
                ('Smartphone X', 'Latest smartphone', 899.99, 'Electronics', 100),
                ('Office Chair', 'Ergonomic office chair', 299.99, 'Furniture', 25)
                """);
                
        stmt.execute("""
                INSERT INTO test_orders (user_id, product_id, quantity, total_amount, order_status) VALUES
                (1, 1, 1, 1999.99, 'COMPLETED'),
                (2, 2, 1, 899.99, 'PENDING'),
                (3, 3, 1, 299.99, 'CANCELLED')
                """);
    }
    
    @Test
    @DisplayName("获取数据源连接信息")
    void testGetDataSourceMetadata() {
        // When
        DataSourceConnection.ConnectionInfo metadata = dataSourceService.getDataSourceMetadata(
                testDataSource.getId(), TEST_USER_ID);
        
        // Then
        assertNotNull(metadata);
        assertNotNull(metadata.getUrl());
        assertNotNull(metadata.getVersion());
        assertNotNull(metadata.getProperties());
        
        // 验证H2数据库特定的元数据
        Map<String, Object> properties = metadata.getProperties();
        assertTrue(properties.containsKey("databaseProductName"));
        assertTrue(properties.containsKey("driverName"));
    }
    
    @Test
    @DisplayName("获取数据库列表")
    void testGetDatabases() {
        // When
        List<String> databases = dataSourceService.getDatabases(testDataSource.getId(), TEST_USER_ID);
        
        // Then
        assertNotNull(databases);
        // H2内存数据库可能返回空列表或包含默认数据库
        assertTrue(databases.isEmpty() || databases.contains("TESTDB"));
    }
    
    @Test
    @DisplayName("获取表列表")
    void testGetTables() {
        // When
        List<DataSourceConnection.TableInfo> tables = dataSourceService.getTables(
                testDataSource.getId(), null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(tables);
        assertTrue(tables.size() >= 3); // 至少包含我们创建的3个测试表
        
        // 验证包含我们的测试表
        List<String> tableNames = tables.stream()
                .map(DataSourceConnection.TableInfo::getName)
                .toList();
                
        assertTrue(tableNames.contains("TEST_USERS") || tableNames.contains("test_users"));
        assertTrue(tableNames.contains("TEST_PRODUCTS") || tableNames.contains("test_products"));
        assertTrue(tableNames.contains("TEST_ORDERS") || tableNames.contains("test_orders"));
        
        // 验证表信息完整性
        for (DataSourceConnection.TableInfo table : tables) {
            assertNotNull(table.getName());
            assertNotNull(table.getType());
            // comment可能为null，这是正常的
        }
    }
    
    @Test
    @DisplayName("获取用户表结构")
    void testGetUserTableSchema() {
        // When
        DataSourceConnection.TableSchema schema = dataSourceService.getTableSchema(
                testDataSource.getId(), "TEST_USERS", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(schema);
        assertEquals("TEST_USERS", schema.getTableName());
        
        // 验证列信息
        List<DataSourceConnection.ColumnInfo> columns = schema.getColumns();
        assertNotNull(columns);
        assertTrue(columns.size() >= 7); // 我们定义的7个字段
        
        // 验证关键字段存在
        List<String> columnNames = columns.stream()
                .map(DataSourceConnection.ColumnInfo::getName)
                .toList();
                
        assertTrue(columnNames.contains("ID"));
        assertTrue(columnNames.contains("USERNAME"));
        assertTrue(columnNames.contains("EMAIL"));
        assertTrue(columnNames.contains("FULL_NAME"));
        assertTrue(columnNames.contains("AGE"));
        assertTrue(columnNames.contains("IS_ACTIVE"));
        
        // 验证列属性
        DataSourceConnection.ColumnInfo idColumn = columns.stream()
                .filter(col -> "ID".equals(col.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(idColumn);
        assertFalse(idColumn.isNullable()); // ID字段应该不允许为空
        
        // 验证索引信息
        List<DataSourceConnection.IndexInfo> indexes = schema.getIndexes();
        assertNotNull(indexes);
        assertTrue(indexes.size() > 0); // 应该有主键索引和我们创建的索引
    }
    
    @Test
    @DisplayName("获取表字段信息")
    void testGetTableColumns() {
        // When
        List<DataSourceConnection.ColumnInfo> columns = dataSourceService.getTableColumns(
                testDataSource.getId(), "TEST_USERS", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(columns);
        assertTrue(columns.size() >= 7);
        
        // 查找用户名字段并验证其属性
        DataSourceConnection.ColumnInfo usernameColumn = columns.stream()
                .filter(col -> "USERNAME".equals(col.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(usernameColumn);
        assertNotNull(usernameColumn.getType());
        assertFalse(usernameColumn.isNullable()); // username字段不允许为空
        
        // 查找年龄字段并验证其属性
        DataSourceConnection.ColumnInfo ageColumn = columns.stream()
                .filter(col -> "AGE".equals(col.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(ageColumn);
        assertTrue(ageColumn.isNullable()); // age字段允许为空
    }
    
    @Test
    @DisplayName("获取表索引信息")
    void testGetTableIndexes() {
        // When
        List<DataSourceConnection.IndexInfo> indexes = dataSourceService.getTableIndexes(
                testDataSource.getId(), "TEST_USERS", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(indexes);
        assertTrue(indexes.size() > 0);
        
        // 验证索引基本信息
        for (DataSourceConnection.IndexInfo index : indexes) {
            assertNotNull(index.getName());
            assertNotNull(index.getType());
            assertNotNull(index.getColumns());
            assertTrue(index.getColumns().size() > 0);
        }
        
        // 查找用户名索引
        List<String> indexNames = indexes.stream()
                .map(DataSourceConnection.IndexInfo::getName)
                .toList();
                
        boolean hasUsernameIndex = indexNames.stream()
                .anyMatch(name -> name.contains("USERNAME") || name.contains("IDX_USERS_USERNAME"));
        assertTrue(hasUsernameIndex, "应该包含用户名索引");
    }
    
    @Test
    @DisplayName("使用元数据服务获取表信息")
    void testMetadataServiceGetTables() {
        // When
        List<DataSourceConnection.TableInfo> tables = metadataService.getTables(
                testDataSource, null, null);
        
        // Then
        assertNotNull(tables);
        assertTrue(tables.size() >= 3);
        
        // 验证表信息
        boolean hasUsersTable = tables.stream()
                .anyMatch(table -> table.getName().equalsIgnoreCase("test_users"));
        assertTrue(hasUsersTable, "应该包含用户表");
    }
    
    @Test
    @DisplayName("使用元数据服务获取表结构")
    void testMetadataServiceGetTableSchema() {
        // When
        DataSourceConnection.TableSchema schema = metadataService.getTableSchema(
                testDataSource, "TEST_USERS", null, null);
        
        // Then
        assertNotNull(schema);
        assertNotNull(schema.getColumns());
        assertNotNull(schema.getIndexes());
        
        assertTrue(schema.getColumns().size() >= 7);
        assertTrue(schema.getIndexes().size() > 0);
    }
    
    @Test
    @DisplayName("获取表统计信息")
    void testGetTableStatistics() {
        // When
        Map<String, Object> stats = metadataService.getTableStatistics(
                testDataSource, "TEST_USERS", null, null);
        
        // Then
        assertNotNull(stats);
        assertTrue(stats.containsKey("tableName"));
        assertEquals("TEST_USERS", stats.get("tableName"));
        
        // 如果成功获取了行数统计
        if (stats.containsKey("rowCount")) {
            Object rowCount = stats.get("rowCount");
            assertNotNull(rowCount);
            assertTrue(((Number) rowCount).longValue() >= 0);
        }
    }
    
    @Test
    @DisplayName("获取表样本数据")
    void testGetTableSampleData() {
        // When
        List<Map<String, Object>> sampleData = metadataService.getTableSampleData(
                testDataSource, "TEST_USERS", null, null, 5);
        
        // Then
        assertNotNull(sampleData);
        assertTrue(sampleData.size() <= 5); // 不应该超过限制数量
        
        if (!sampleData.isEmpty()) {
            Map<String, Object> firstRow = sampleData.get(0);
            assertNotNull(firstRow);
            assertTrue(firstRow.containsKey("ID") || firstRow.containsKey("id"));
            assertTrue(firstRow.containsKey("USERNAME") || firstRow.containsKey("username"));
        }
    }
    
    @Test
    @DisplayName("验证表是否存在")
    void testTableExists() {
        // When - 获取所有表
        List<DataSourceConnection.TableInfo> tables = metadataService.getTables(testDataSource, null, null);
        
        // Then - 存在的表
        boolean usersTableExists = tables.stream()
                .anyMatch(table -> "TEST_USERS".equalsIgnoreCase(table.getName()));
        assertTrue(usersTableExists, "TEST_USERS表应该存在");
        
        // Then - 不存在的表
        boolean nonExistentTableExists = tables.stream()
                .anyMatch(table -> "NON_EXISTENT_TABLE".equalsIgnoreCase(table.getName()));
        assertFalse(nonExistentTableExists, "NON_EXISTENT_TABLE表不应该存在");
    }
    
    @Test
    @DisplayName("测试权限检查")
    void testMetadataPermissionCheck() {
        // Given - 其他用户ID
        Long otherUserId = 999L;
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> dataSourceService.getDataSourceMetadata(testDataSource.getId(), otherUserId));
        assertEquals("无权访问该数据源", exception.getMessage());
    }
}