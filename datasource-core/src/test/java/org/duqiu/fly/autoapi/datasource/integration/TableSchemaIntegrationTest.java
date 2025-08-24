package org.duqiu.fly.autoapi.datasource.integration;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 表结构和索引查询详细集成测试
 */
@DisplayName("表结构和索引查询集成测试")
public class TableSchemaIntegrationTest extends DataSourceIntegrationTestBase {
    
    private DataSource testDataSource;
    
    @BeforeEach
    void setUpSchemaTest() throws SQLException {
        testDataSource = createTestDataSource();
        testDataSource.setConnectionUrl(testDatabaseUrl);
        testDataSource = dataSourceRepository.save(testDataSource);
        
        // 创建复杂的测试表结构
        createComplexTestSchema();
    }
    
    private void createComplexTestSchema() throws SQLException {
        try (Connection conn = getTestConnection()) {
            Statement stmt = conn.createStatement();
            
            // 删除已存在的表
            stmt.execute("DROP TABLE IF EXISTS order_items");
            stmt.execute("DROP TABLE IF EXISTS customer_orders");
            stmt.execute("DROP TABLE IF EXISTS customers");
            stmt.execute("DROP TABLE IF EXISTS product_categories");
            
            // 创建产品分类表
            stmt.execute("""
                    CREATE TABLE product_categories (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        category_name VARCHAR(100) NOT NULL UNIQUE,
                        parent_id BIGINT NULL,
                        description TEXT,
                        display_order INT DEFAULT 0,
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
            
            // 创建客户表
            stmt.execute("""
                    CREATE TABLE customers (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        customer_code VARCHAR(20) NOT NULL UNIQUE,
                        company_name VARCHAR(200) NOT NULL,
                        contact_person VARCHAR(100),
                        phone VARCHAR(20),
                        email VARCHAR(100),
                        address TEXT,
                        city VARCHAR(50),
                        state VARCHAR(50),
                        postal_code VARCHAR(20),
                        country VARCHAR(50) DEFAULT 'China',
                        credit_limit DECIMAL(15,2) DEFAULT 0.00,
                        customer_type ENUM('INDIVIDUAL', 'CORPORATE', 'GOVERNMENT') DEFAULT 'INDIVIDUAL',
                        registration_date DATE,
                        is_vip BOOLEAN DEFAULT FALSE,
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
            
            // 创建客户订单表
            stmt.execute("""
                    CREATE TABLE customer_orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_number VARCHAR(50) NOT NULL UNIQUE,
                        customer_id BIGINT NOT NULL,
                        order_date DATE NOT NULL,
                        required_date DATE,
                        shipped_date DATE,
                        order_status ENUM('DRAFT', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') DEFAULT 'DRAFT',
                        priority_level ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL',
                        subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                        tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                        shipping_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                        total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                        discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
                        payment_method VARCHAR(50),
                        payment_status ENUM('PENDING', 'PARTIAL', 'PAID', 'REFUNDED') DEFAULT 'PENDING',
                        shipping_address TEXT,
                        notes TEXT,
                        created_by VARCHAR(50),
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE RESTRICT
                    )
                    """);
            
            // 创建订单明细表
            stmt.execute("""
                    CREATE TABLE order_items (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_id BIGINT NOT NULL,
                        product_code VARCHAR(50) NOT NULL,
                        product_name VARCHAR(200) NOT NULL,
                        category_id BIGINT,
                        quantity INT NOT NULL CHECK (quantity > 0),
                        unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
                        discount_percent DECIMAL(5,2) DEFAULT 0.00 CHECK (discount_percent >= 0 AND discount_percent <= 100),
                        line_total DECIMAL(12,2) AS (quantity * unit_price * (1 - discount_percent / 100)) STORED,
                        specifications JSON,
                        delivery_date DATE,
                        item_status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (order_id) REFERENCES customer_orders(id) ON DELETE CASCADE,
                        FOREIGN KEY (category_id) REFERENCES product_categories(id) ON DELETE SET NULL
                    )
                    """);
            
            // 创建各种类型的索引
            createIndexes(stmt);
            
            // 插入测试数据
            insertTestData(stmt);
        }
    }
    
    private void createIndexes(Statement stmt) throws SQLException {
        // 普通索引
        stmt.execute("CREATE INDEX idx_customers_company_name ON customers(company_name)");
        stmt.execute("CREATE INDEX idx_customers_city ON customers(city)");
        stmt.execute("CREATE INDEX idx_customers_customer_type ON customers(customer_type)");
        stmt.execute("CREATE INDEX idx_orders_order_date ON customer_orders(order_date)");
        stmt.execute("CREATE INDEX idx_orders_status ON customer_orders(order_status)");
        stmt.execute("CREATE INDEX idx_items_product_code ON order_items(product_code)");
        
        // 复合索引
        stmt.execute("CREATE INDEX idx_customers_city_type ON customers(city, customer_type)");
        stmt.execute("CREATE INDEX idx_orders_customer_date ON customer_orders(customer_id, order_date)");
        stmt.execute("CREATE INDEX idx_orders_status_priority ON customer_orders(order_status, priority_level)");
        stmt.execute("CREATE INDEX idx_items_order_product ON order_items(order_id, product_code)");
        stmt.execute("CREATE INDEX idx_items_category_status ON order_items(category_id, item_status)");
        
        // 唯一索引
        stmt.execute("CREATE UNIQUE INDEX uk_customers_email ON customers(email)");
        stmt.execute("CREATE UNIQUE INDEX uk_category_name_parent ON product_categories(category_name, parent_id)");
        
        // 条件索引（部分索引）- H2可能不完全支持，但我们可以尝试
        stmt.execute("CREATE INDEX idx_customers_active_vip ON customers(is_vip) WHERE is_active = TRUE");
        stmt.execute("CREATE INDEX idx_orders_pending_high_priority ON customer_orders(priority_level) WHERE order_status = 'DRAFT'");
    }
    
    private void insertTestData(Statement stmt) throws SQLException {
        // 插入产品分类
        stmt.execute("""
                INSERT INTO product_categories (category_name, description, display_order) VALUES
                ('Electronics', 'Electronic products and gadgets', 1),
                ('Furniture', 'Office and home furniture', 2),
                ('Books', 'Books and educational materials', 3),
                ('Sports', 'Sports equipment and accessories', 4)
                """);
        
        // 插入客户数据
        stmt.execute("""
                INSERT INTO customers (customer_code, company_name, contact_person, phone, email, city, customer_type, credit_limit, is_vip) VALUES
                ('CUST001', 'Tech Solutions Ltd', 'John Smith', '1234567890', 'john@techsolutions.com', 'Beijing', 'CORPORATE', 50000.00, true),
                ('CUST002', 'Global Trading Co', 'Jane Doe', '1234567891', 'jane@globaltrading.com', 'Shanghai', 'CORPORATE', 30000.00, false),
                ('CUST003', 'Individual Customer 1', 'Bob Wilson', '1234567892', 'bob@email.com', 'Guangzhou', 'INDIVIDUAL', 5000.00, false),
                ('CUST004', 'Government Office', 'Alice Brown', '1234567893', 'alice@gov.org', 'Shenzhen', 'GOVERNMENT', 100000.00, true)
                """);
        
        // 插入订单数据
        stmt.execute("""
                INSERT INTO customer_orders (order_number, customer_id, order_date, order_status, priority_level, subtotal, total_amount, payment_method) VALUES
                ('ORD2024001', 1, '2024-01-15', 'DELIVERED', 'HIGH', 15000.00, 16500.00, 'CREDIT_CARD'),
                ('ORD2024002', 2, '2024-01-20', 'SHIPPED', 'NORMAL', 8000.00, 8800.00, 'BANK_TRANSFER'),
                ('ORD2024003', 1, '2024-01-25', 'PROCESSING', 'URGENT', 25000.00, 27500.00, 'CREDIT_CARD'),
                ('ORD2024004', 3, '2024-01-30', 'CONFIRMED', 'LOW', 1200.00, 1320.00, 'CASH'),
                ('ORD2024005', 4, '2024-02-05', 'DRAFT', 'HIGH', 45000.00, 49500.00, 'GOVERNMENT_PURCHASE')
                """);
        
        // 插入订单明细数据
        stmt.execute("""
                INSERT INTO order_items (order_id, product_code, product_name, category_id, quantity, unit_price, discount_percent) VALUES
                (1, 'LAPTOP001', 'High Performance Laptop', 1, 10, 1500.00, 5.00),
                (1, 'MOUSE001', 'Wireless Mouse', 1, 10, 50.00, 0.00),
                (2, 'DESK001', 'Standing Desk', 2, 4, 2000.00, 10.00),
                (3, 'LAPTOP002', 'Gaming Laptop', 1, 15, 1800.00, 8.00),
                (3, 'CHAIR001', 'Ergonomic Chair', 2, 5, 500.00, 0.00),
                (4, 'BOOK001', 'Programming Book', 3, 6, 200.00, 0.00),
                (5, 'SERVER001', 'Enterprise Server', 1, 3, 15000.00, 0.00)
                """);
    }
    
    @Test
    @DisplayName("获取复杂表结构 - 客户表")
    void testGetComplexTableSchema_Customers() {
        // When
        DataSourceConnection.TableSchema schema = dataSourceService.getTableSchema(
                testDataSource.getId(), "CUSTOMERS", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(schema);
        assertEquals("CUSTOMERS", schema.getTableName());
        
        // 验证字段数量和类型
        List<DataSourceConnection.ColumnInfo> columns = schema.getColumns();
        assertNotNull(columns);
        assertTrue(columns.size() >= 17); // 客户表有17个字段
        
        // 验证关键字段
        Map<String, DataSourceConnection.ColumnInfo> columnMap = columns.stream()
                .collect(Collectors.toMap(DataSourceConnection.ColumnInfo::getName, col -> col));
        
        // 主键字段
        assertTrue(columnMap.containsKey("ID"));
        assertFalse(columnMap.get("ID").isNullable());
        
        // 唯一字段
        assertTrue(columnMap.containsKey("CUSTOMER_CODE"));
        assertFalse(columnMap.get("CUSTOMER_CODE").isNullable());
        
        // 可空字段
        assertTrue(columnMap.containsKey("CONTACT_PERSON"));
        assertTrue(columnMap.get("CONTACT_PERSON").isNullable());
        
        // 枚举字段
        assertTrue(columnMap.containsKey("CUSTOMER_TYPE"));
        
        // 数值字段
        assertTrue(columnMap.containsKey("CREDIT_LIMIT"));
        
        // 验证索引
        List<DataSourceConnection.IndexInfo> indexes = schema.getIndexes();
        assertNotNull(indexes);
        assertTrue(indexes.size() > 0);
        
        // 验证特定索引存在
        List<String> indexNames = indexes.stream()
                .map(idx -> idx.getName().toUpperCase())
                .toList();
        
        boolean hasCompanyNameIndex = indexNames.stream()
                .anyMatch(name -> name.contains("COMPANY_NAME"));
        assertTrue(hasCompanyNameIndex, "应该包含公司名称索引");
    }
    
    @Test
    @DisplayName("获取外键关系表结构 - 订单表")
    void testGetTableSchemaWithForeignKeys_Orders() {
        // When
        DataSourceConnection.TableSchema schema = dataSourceService.getTableSchema(
                testDataSource.getId(), "CUSTOMER_ORDERS", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(schema);
        
        List<DataSourceConnection.ColumnInfo> columns = schema.getColumns();
        assertTrue(columns.size() >= 20); // 订单表有20+个字段
        
        // 验证外键字段
        Map<String, DataSourceConnection.ColumnInfo> columnMap = columns.stream()
                .collect(Collectors.toMap(DataSourceConnection.ColumnInfo::getName, col -> col));
        
        assertTrue(columnMap.containsKey("CUSTOMER_ID"));
        assertFalse(columnMap.get("CUSTOMER_ID").isNullable());
        
        // 验证枚举字段
        assertTrue(columnMap.containsKey("ORDER_STATUS"));
        assertTrue(columnMap.containsKey("PRIORITY_LEVEL"));
        assertTrue(columnMap.containsKey("PAYMENT_STATUS"));
        
        // 验证计算字段和约束
        assertTrue(columnMap.containsKey("TOTAL_AMOUNT"));
        
        // 验证复合索引
        List<DataSourceConnection.IndexInfo> indexes = schema.getIndexes();
        boolean hasCompoundIndex = indexes.stream()
                .anyMatch(idx -> idx.getColumns().size() > 1);
        assertTrue(hasCompoundIndex, "应该包含复合索引");
    }
    
    @Test
    @DisplayName("获取包含计算字段的表结构 - 订单明细表")
    void testGetTableSchemaWithComputedColumns_OrderItems() {
        // When
        DataSourceConnection.TableSchema schema = dataSourceService.getTableSchema(
                testDataSource.getId(), "ORDER_ITEMS", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(schema);
        
        List<DataSourceConnection.ColumnInfo> columns = schema.getColumns();
        Map<String, DataSourceConnection.ColumnInfo> columnMap = columns.stream()
                .collect(Collectors.toMap(DataSourceConnection.ColumnInfo::getName, col -> col));
        
        // 验证普通字段
        assertTrue(columnMap.containsKey("QUANTITY"));
        assertTrue(columnMap.containsKey("UNIT_PRICE"));
        assertTrue(columnMap.containsKey("DISCOUNT_PERCENT"));
        
        // 验证计算字段（如果H2支持）
        assertTrue(columnMap.containsKey("LINE_TOTAL"));
        
        // 验证JSON字段（如果H2支持）
        assertTrue(columnMap.containsKey("SPECIFICATIONS"));
        
        // 验证外键约束
        assertTrue(columnMap.containsKey("ORDER_ID"));
        assertTrue(columnMap.containsKey("CATEGORY_ID"));
    }
    
    @Test
    @DisplayName("验证索引类型和属性")
    void testIndexTypesAndProperties() {
        // When
        List<DataSourceConnection.IndexInfo> indexes = dataSourceService.getTableIndexes(
                testDataSource.getId(), "CUSTOMERS", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(indexes);
        assertTrue(indexes.size() > 0);
        
        // 验证索引基本属性
        for (DataSourceConnection.IndexInfo index : indexes) {
            assertNotNull(index.getName());
            assertNotNull(index.getType());
            assertNotNull(index.getColumns());
            assertFalse(index.getColumns().isEmpty());
            
            // 验证列名不为空
            for (String column : index.getColumns()) {
                assertNotNull(column);
                assertFalse(column.trim().isEmpty());
            }
        }
        
        // 查找唯一索引
        List<DataSourceConnection.IndexInfo> uniqueIndexes = indexes.stream()
                .filter(DataSourceConnection.IndexInfo::isUnique)
                .toList();
        assertTrue(uniqueIndexes.size() > 0, "应该包含唯一索引");
        
        // 查找复合索引
        List<DataSourceConnection.IndexInfo> compoundIndexes = indexes.stream()
                .filter(idx -> idx.getColumns().size() > 1)
                .toList();
        assertTrue(compoundIndexes.size() > 0, "应该包含复合索引");
    }
    
    @Test
    @DisplayName("验证不同表的索引分布")
    void testIndexDistributionAcrossTables() {
        // When - 获取所有表的索引
        List<DataSourceConnection.TableInfo> tables = dataSourceService.getTables(
                testDataSource.getId(), null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(tables);
        assertTrue(tables.size() >= 4); // 至少包含4个测试表
        
        for (DataSourceConnection.TableInfo table : tables) {
            String tableName = table.getName();
            if (tableName.contains("CUSTOMER") || tableName.contains("ORDER")) {
                // 获取表的索引
                List<DataSourceConnection.IndexInfo> indexes = dataSourceService.getTableIndexes(
                        testDataSource.getId(), tableName, null, null, TEST_USER_ID);
                
                assertNotNull(indexes, "表 " + tableName + " 应该有索引");
                assertTrue(indexes.size() > 0, "表 " + tableName + " 应该至少有一个索引（主键）");
            }
        }
    }
    
    @Test
    @DisplayName("验证字段类型映射")
    void testColumnTypeMapping() {
        // When
        List<DataSourceConnection.ColumnInfo> columns = dataSourceService.getTableColumns(
                testDataSource.getId(), "CUSTOMERS", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(columns);
        
        Map<String, DataSourceConnection.ColumnInfo> columnMap = columns.stream()
                .collect(Collectors.toMap(DataSourceConnection.ColumnInfo::getName, col -> col));
        
        // 验证不同数据类型
        DataSourceConnection.ColumnInfo idColumn = columnMap.get("ID");
        assertNotNull(idColumn);
        assertTrue(idColumn.getType().contains("BIGINT") || idColumn.getType().contains("INTEGER"));
        
        DataSourceConnection.ColumnInfo nameColumn = columnMap.get("COMPANY_NAME");
        assertNotNull(nameColumn);
        assertTrue(nameColumn.getType().contains("VARCHAR") || nameColumn.getType().contains("CHARACTER"));
        
        DataSourceConnection.ColumnInfo creditColumn = columnMap.get("CREDIT_LIMIT");
        assertNotNull(creditColumn);
        assertTrue(creditColumn.getType().contains("DECIMAL") || creditColumn.getType().contains("NUMERIC"));
        
        DataSourceConnection.ColumnInfo activeColumn = columnMap.get("IS_ACTIVE");
        assertNotNull(activeColumn);
        assertTrue(activeColumn.getType().contains("BOOLEAN") || activeColumn.getType().contains("BOOL"));
        
        DataSourceConnection.ColumnInfo timestampColumn = columnMap.get("CREATED_AT");
        assertNotNull(timestampColumn);
        assertTrue(timestampColumn.getType().contains("TIMESTAMP") || timestampColumn.getType().contains("DATETIME"));
    }
    
    @Test
    @DisplayName("测试大表的元数据查询性能")
    void testLargeTableMetadataPerformance() throws SQLException {
        // Given - 创建一个包含更多字段和索引的表
        try (Connection conn = getTestConnection()) {
            Statement stmt = conn.createStatement();
            
            stmt.execute("DROP TABLE IF EXISTS large_test_table");
            
            // 创建包含50个字段的表
            StringBuilder createTableSql = new StringBuilder();
            createTableSql.append("CREATE TABLE large_test_table (\n");
            createTableSql.append("    id BIGINT AUTO_INCREMENT PRIMARY KEY,\n");
            
            for (int i = 1; i <= 48; i++) {
                createTableSql.append(String.format("    field_%02d VARCHAR(100),\n", i));
            }
            
            createTableSql.append("    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n");
            createTableSql.append(")");
            
            stmt.execute(createTableSql.toString());
            
            // 创建多个索引
            for (int i = 1; i <= 10; i++) {
                stmt.execute(String.format("CREATE INDEX idx_field_%02d ON large_test_table(field_%02d)", i, i));
            }
            
            // 创建复合索引
            stmt.execute("CREATE INDEX idx_compound_1 ON large_test_table(field_01, field_02, field_03)");
            stmt.execute("CREATE INDEX idx_compound_2 ON large_test_table(field_04, field_05)");
        }
        
        // When - 测试查询性能
        long startTime = System.currentTimeMillis();
        
        DataSourceConnection.TableSchema schema = dataSourceService.getTableSchema(
                testDataSource.getId(), "LARGE_TEST_TABLE", null, null, TEST_USER_ID);
        
        long endTime = System.currentTimeMillis();
        long queryTime = endTime - startTime;
        
        // Then
        assertNotNull(schema);
        assertEquals("LARGE_TEST_TABLE", schema.getTableName());
        
        List<DataSourceConnection.ColumnInfo> columns = schema.getColumns();
        assertEquals(49, columns.size()); // 49个字段（包括ID和created_at）
        
        List<DataSourceConnection.IndexInfo> indexes = schema.getIndexes();
        assertTrue(indexes.size() >= 12); // 至少12个索引
        
        // 验证查询时间合理（应该在5秒内完成）
        assertTrue(queryTime < 5000, "大表元数据查询时间应该在5秒内: " + queryTime + "ms");
        
        System.out.println("大表元数据查询耗时: " + queryTime + "ms");
        System.out.println("字段数量: " + columns.size());
        System.out.println("索引数量: " + indexes.size());
    }
}