package org.duqiu.fly.autoapi.service.integration;

import org.duqiu.fly.autoapi.api.dto.ApiServiceCreateRequest;
import org.duqiu.fly.autoapi.api.dto.ApiServiceResponse;
import org.duqiu.fly.autoapi.api.dto.TableSelectionRequest;
import org.duqiu.fly.autoapi.api.service.EnhancedApiServiceService;
import org.duqiu.fly.autoapi.api.service.TableSelectionService;
import org.duqiu.fly.autoapi.service.util.ApiServiceTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.duqiu.fly.autoapi.service.util.ApiServiceTestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 表选择功能集成测试
 */
@DisplayName("表选择功能集成测试")
public class TableSelectionIntegrationTest extends ApiServiceIntegrationTestBase {
    
    @Autowired
    private EnhancedApiServiceService apiServiceService;
    
    @Autowired
    private TableSelectionService tableSelectionService;
    
    @Test
    @DisplayName("保存表选择配置 - 基本配置")
    void testSaveTableSelections_Basic() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        List<TableSelectionRequest> selections = ApiServiceTestFactory.createTableSelectionRequests();
        
        // When
        tableSelectionService.saveTableSelections(created.getId(), selections, TEST_USER_ID);
        
        // Then
        List<TableSelectionRequest> saved = tableSelectionService.getTableSelections(created.getId());
        assertNotNull(saved);
        assertEquals(selections.size(), saved.size());
        
        // 验证每个表选择配置
        for (int i = 0; i < selections.size(); i++) {
            assertTableSelection(saved.get(i), selections.get(i));
        }
    }
    
    @Test
    @DisplayName("保存表选择配置 - 复杂配置")
    void testSaveTableSelections_Complex() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createComplexApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        List<TableSelectionRequest> selections = ApiServiceTestFactory.createComplexTableSelectionRequests();
        
        // When
        tableSelectionService.saveTableSelections(created.getId(), selections, TEST_USER_ID);
        
        // Then
        List<TableSelectionRequest> saved = tableSelectionService.getTableSelections(created.getId());
        assertNotNull(saved);
        assertEquals(3, saved.size()); // orders, users, products
        
        // 验证主表
        TableSelectionRequest primaryTable = saved.stream()
                .filter(s -> s.getIsPrimary())
                .findFirst()
                .orElse(null);
        assertNotNull(primaryTable);
        assertEquals("test_orders", primaryTable.getTableName());
        assertEquals("o", primaryTable.getTableAlias());
        
        // 验证关联表
        List<TableSelectionRequest> joinTables = saved.stream()
                .filter(s -> !s.getIsPrimary())
                .toList();
        assertEquals(2, joinTables.size());
        
        // 验证JOIN条件
        boolean hasUserJoin = joinTables.stream()
                .anyMatch(t -> t.getJoinCondition().contains("user_id"));
        assertTrue(hasUserJoin);
        
        boolean hasProductJoin = joinTables.stream()
                .anyMatch(t -> t.getJoinCondition().contains("product_id"));
        assertTrue(hasProductJoin);
    }
    
    @Test
    @DisplayName("更新表选择配置")
    void testUpdateTableSelections() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        List<TableSelectionRequest> initialSelections = ApiServiceTestFactory.createTableSelectionRequests();
        tableSelectionService.saveTableSelections(created.getId(), initialSelections, TEST_USER_ID);
        
        // 修改配置
        List<TableSelectionRequest> updatedSelections = ApiServiceTestFactory.createComplexTableSelectionRequests();
        
        // When
        tableSelectionService.saveTableSelections(created.getId(), updatedSelections, TEST_USER_ID);
        
        // Then
        List<TableSelectionRequest> saved = tableSelectionService.getTableSelections(created.getId());
        assertEquals(updatedSelections.size(), saved.size());
        
        // 验证配置已更新
        boolean hasOrdersTable = saved.stream()
                .anyMatch(s -> s.getTableName().equals("test_orders"));
        assertTrue(hasOrdersTable);
        
        // 原来的配置应该被完全替换
        long usersTableCount = saved.stream()
                .filter(s -> s.getTableName().equals("test_users"))
                .count();
        assertEquals(1, usersTableCount); // 新配置中仍有users表
    }
    
    @Test
    @DisplayName("获取数据源表列表")
    void testGetDataSourceTables() {
        // When
        List<Map<String, Object>> tables = tableSelectionService.getDataSourceTables(
                1L, null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(tables);
        assertFalse(tables.isEmpty());
        
        // 验证包含测试表
        List<String> tableNames = tables.stream()
                .map(t -> (String) t.get("name"))
                .toList();
        
        assertTrue(tableNames.contains("test_users"));
        assertTrue(tableNames.contains("test_orders"));
        assertTrue(tableNames.contains("test_products"));
        assertTrue(tableNames.contains("test_categories"));
        
        // 验证表信息结构
        Map<String, Object> firstTable = tables.get(0);
        assertNotNull(firstTable.get("name"));
        assertTrue(firstTable.containsKey("type"));
        assertTrue(firstTable.containsKey("comment"));
    }
    
    @Test
    @DisplayName("获取数据源表列表 - 无权限")
    void testGetDataSourceTables_NoPermission() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            tableSelectionService.getDataSourceTables(1L, null, null, OTHER_USER_ID);
        });
    }
    
    @Test
    @DisplayName("获取表字段列表")
    void testGetTableColumns() {
        // When
        List<Map<String, Object>> columns = tableSelectionService.getTableColumns(
                1L, "test_users", null, null, TEST_USER_ID);
        
        // Then
        assertNotNull(columns);
        assertFalse(columns.isEmpty());
        
        // 验证包含预期字段
        List<String> columnNames = columns.stream()
                .map(c -> (String) c.get("name"))
                .toList();
        
        assertTrue(columnNames.contains("id"));
        assertTrue(columnNames.contains("username"));
        assertTrue(columnNames.contains("email"));
        assertTrue(columnNames.contains("status"));
        
        // 验证字段信息结构
        Map<String, Object> firstColumn = columns.get(0);
        assertNotNull(firstColumn.get("name"));
        assertNotNull(firstColumn.get("type"));
        assertTrue(firstColumn.containsKey("nullable"));
        assertTrue(firstColumn.containsKey("comment"));
        assertTrue(firstColumn.containsKey("defaultValue"));
        assertTrue(firstColumn.containsKey("isPrimaryKey"));
    }
    
    @Test
    @DisplayName("获取表字段列表 - 表不存在")
    void testGetTableColumns_TableNotFound() {
        // When & Then
        assertThrows(RuntimeException.class, () -> {
            tableSelectionService.getTableColumns(1L, "non_existent_table", null, null, TEST_USER_ID);
        });
    }
    
    @Test
    @DisplayName("生成SQL模板 - 简单查询")
    void testGenerateSqlTemplate_Simple() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        List<TableSelectionRequest> selections = ApiServiceTestFactory.createTableSelectionRequests();
        tableSelectionService.saveTableSelections(created.getId(), selections, TEST_USER_ID);
        
        // When
        String sqlTemplate = tableSelectionService.generateSqlTemplate(created.getId());
        
        // Then
        assertSqlTemplate(sqlTemplate, selections);
        assertStringContainsAll(sqlTemplate, 
            "SELECT", "FROM", "test_users", "LEFT JOIN", "test_orders");
        
        // 验证字段列表
        assertTrue(sqlTemplate.contains("u.id"));
        assertTrue(sqlTemplate.contains("u.username"));
        assertTrue(sqlTemplate.contains("o.quantity"));
        
        // 验证WHERE子句模板
        assertTrue(sqlTemplate.contains("WHERE 1=1"));
        assertTrue(sqlTemplate.contains("${paramName}"));
    }
    
    @Test
    @DisplayName("生成SQL模板 - 复杂JOIN查询")
    void testGenerateSqlTemplate_ComplexJoin() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createComplexApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        List<TableSelectionRequest> selections = ApiServiceTestFactory.createComplexTableSelectionRequests();
        tableSelectionService.saveTableSelections(created.getId(), selections, TEST_USER_ID);
        
        // When
        String sqlTemplate = tableSelectionService.generateSqlTemplate(created.getId());
        
        // Then
        assertSqlTemplate(sqlTemplate, selections);
        
        // 验证复杂JOIN结构
        assertStringContainsAll(sqlTemplate,
            "FROM test_orders AS o",
            "INNER JOIN test_users AS u ON o.user_id = u.id",
            "INNER JOIN test_products AS p ON o.product_id = p.id"
        );
        
        // 验证字段别名
        assertTrue(sqlTemplate.contains("o.id"));
        assertTrue(sqlTemplate.contains("u.username"));
        assertTrue(sqlTemplate.contains("p.name"));
    }
    
    @Test
    @DisplayName("生成SQL模板 - 空配置")
    void testGenerateSqlTemplate_EmptyConfig() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When - 没有配置表选择
        String sqlTemplate = tableSelectionService.generateSqlTemplate(created.getId());
        
        // Then
        assertNotNull(sqlTemplate);
        assertEquals("SELECT * FROM your_table", sqlTemplate);
    }
    
    @Test
    @DisplayName("表选择配置持久化测试")
    void testTableSelectionPersistence() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        List<TableSelectionRequest> selections = ApiServiceTestFactory.createComplexTableSelectionRequests();
        
        // When
        tableSelectionService.saveTableSelections(created.getId(), selections, TEST_USER_ID);
        
        // Then - 验证数据库中的记录
        var savedEntities = tableSelectionRepository.findByApiServiceIdOrderBySortOrder(created.getId());
        assertEquals(selections.size(), savedEntities.size());
        
        // 验证排序
        for (int i = 0; i < savedEntities.size(); i++) {
            assertEquals(i, savedEntities.get(i).getSortOrder().intValue());
            assertEquals(TEST_USER_ID, savedEntities.get(i).getCreatedBy());
        }
        
        // 验证主表标识
        long primaryCount = savedEntities.stream()
                .mapToLong(e -> e.getIsPrimary() ? 1 : 0)
                .sum();
        assertEquals(1, primaryCount); // 只能有一个主表
        
        // 验证JSON字段序列化
        var entityWithColumns = savedEntities.stream()
                .filter(e -> e.getSelectedColumns() != null)
                .findFirst()
                .orElse(null);
        assertNotNull(entityWithColumns);
        assertTrue(entityWithColumns.getSelectedColumns().contains("["));
        assertTrue(entityWithColumns.getSelectedColumns().contains("]"));
    }
    
    @Test
    @DisplayName("字段选择序列化测试")
    void testColumnSelectionSerialization() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        TableSelectionRequest selection = new TableSelectionRequest();
        selection.setTableName("test_users");
        selection.setTableAlias("u");
        selection.setIsPrimary(true);
        selection.setSelectedColumns(List.of("id", "username", "email AS user_email", "status"));
        selection.setSortOrder(0);
        
        List<TableSelectionRequest> selections = List.of(selection);
        
        // When
        tableSelectionService.saveTableSelections(created.getId(), selections, TEST_USER_ID);
        List<TableSelectionRequest> retrieved = tableSelectionService.getTableSelections(created.getId());
        
        // Then
        assertEquals(1, retrieved.size());
        TableSelectionRequest retrievedSelection = retrieved.get(0);
        
        assertNotNull(retrievedSelection.getSelectedColumns());
        assertEquals(4, retrievedSelection.getSelectedColumns().size());
        assertTrue(retrievedSelection.getSelectedColumns().contains("id"));
        assertTrue(retrievedSelection.getSelectedColumns().contains("username"));
        assertTrue(retrievedSelection.getSelectedColumns().contains("email AS user_email"));
        assertTrue(retrievedSelection.getSelectedColumns().contains("status"));
    }
    
    @Test
    @DisplayName("数据源权限验证测试")
    void testDataSourcePermissionValidation() {
        // When & Then - 测试获取表列表权限
        assertThrows(IllegalArgumentException.class, () -> {
            tableSelectionService.getDataSourceTables(1L, null, null, OTHER_USER_ID);
        });
        
        // When & Then - 测试获取字段列表权限
        assertThrows(IllegalArgumentException.class, () -> {
            tableSelectionService.getTableColumns(1L, "test_users", null, null, OTHER_USER_ID);
        });
        
        // When & Then - 测试不存在的数据源
        assertThrows(IllegalArgumentException.class, () -> {
            tableSelectionService.getDataSourceTables(999L, null, null, TEST_USER_ID);
        });
    }
    
    @Test
    @DisplayName("表选择配置删除测试")
    void testTableSelectionDeletion() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        List<TableSelectionRequest> selections = ApiServiceTestFactory.createTableSelectionRequests();
        tableSelectionService.saveTableSelections(created.getId(), selections, TEST_USER_ID);
        
        // 验证配置存在
        assertFalse(tableSelectionService.getTableSelections(created.getId()).isEmpty());
        
        // When - 删除API服务
        apiServiceService.deleteApiService(created.getId(), TEST_USER_ID);
        
        // Then - 验证表选择配置也被删除
        assertEquals(0, tableSelectionRepository.countByApiServiceId(created.getId()));
    }
    
    @Test
    @DisplayName("SQL模板生成边界情况测试")
    void testSqlTemplateGenerationEdgeCases() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // Case 1: 只有主表，没有字段选择
        TableSelectionRequest primaryOnly = new TableSelectionRequest();
        primaryOnly.setTableName("test_users");
        primaryOnly.setIsPrimary(true);
        primaryOnly.setSortOrder(0);
        
        tableSelectionService.saveTableSelections(created.getId(), List.of(primaryOnly), TEST_USER_ID);
        
        // When
        String sqlTemplate = tableSelectionService.generateSqlTemplate(created.getId());
        
        // Then
        assertTrue(sqlTemplate.contains("FROM test_users"));
        assertTrue(sqlTemplate.contains("test_users.*")); // 没有指定字段时使用 *
        
        // Case 2: 多表但没有JOIN条件
        TableSelectionRequest secondary = new TableSelectionRequest();
        secondary.setTableName("test_orders");
        secondary.setIsPrimary(false);
        secondary.setSortOrder(1);
        // 注意：没有设置joinCondition
        
        tableSelectionService.saveTableSelections(
                created.getId(), List.of(primaryOnly, secondary), TEST_USER_ID);
        
        sqlTemplate = tableSelectionService.generateSqlTemplate(created.getId());
        
        // 应该包含第二个表，但JOIN条件可能为空
        assertTrue(sqlTemplate.contains("test_orders"));
    }
}