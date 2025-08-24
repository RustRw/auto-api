package org.duqiu.fly.autoapi.datasource.util;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponseV2;
import org.duqiu.fly.autoapi.datasource.model.DataSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试断言工具类 - 提供常用的测试断言方法
 */
public class TestAssertions {
    
    /**
     * 断言数据源响应与请求匹配
     */
    public static void assertDataSourceResponse(DataSourceResponseV2 response, DataSourceCreateRequestV2 request) {
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getType(), response.getType());
        assertEquals(request.getHost(), response.getHost());
        assertEquals(request.getPort(), response.getPort());
        assertEquals(request.getDatabase(), response.getDatabase());
        assertEquals(request.getUsername(), response.getUsername());
        assertEquals(request.getMaxPoolSize(), response.getMaxPoolSize());
        assertEquals(request.getMinPoolSize(), response.getMinPoolSize());
        assertEquals(request.getConnectionTimeout(), response.getConnectionTimeout());
        assertEquals(request.getSslEnabled(), response.getSslEnabled());
        assertEquals(request.getConnectionPoolEnabled(), response.getConnectionPoolEnabled());
        assertNotNull(response.getCreatedAt());
        assertTrue(response.getEnabled());
    }
    
    /**
     * 断言数据源实体与请求匹配
     */
    public static void assertDataSourceEntity(DataSource entity, DataSourceCreateRequestV2 request, Long userId) {
        assertNotNull(entity);
        assertEquals(request.getName(), entity.getName());
        assertEquals(request.getDescription(), entity.getDescription());
        assertEquals(request.getType(), entity.getType());
        assertEquals(request.getHost(), entity.getHost());
        assertEquals(request.getPort(), entity.getPort());
        assertEquals(request.getDatabase(), entity.getDatabase());
        assertEquals(request.getUsername(), entity.getUsername());
        assertEquals(request.getPassword(), entity.getPassword());
        assertEquals(userId, entity.getCreatedBy());
        assertEquals(userId, entity.getUpdatedBy());
        assertTrue(entity.getEnabled());
    }
    
    /**
     * 断言连接状态
     */
    public static void assertConnectionStatus(DataSourceResponseV2.ConnectionStatus status, boolean expectedConnected) {
        assertNotNull(status);
        assertEquals(expectedConnected, status.isConnected());
        assertNotNull(status.getLastTestTime());
        assertNotNull(status.getResponseTime());
        assertTrue(status.getResponseTime() >= 0);
        assertNotNull(status.getMessage());
        assertFalse(status.getMessage().trim().isEmpty());
    }
    
    /**
     * 断言连接状态 - 不验证连接结果，只验证状态结构
     */
    public static void assertConnectionStatusStructure(DataSourceResponseV2.ConnectionStatus status) {
        assertNotNull(status);
        assertNotNull(status.getLastTestTime());
        assertNotNull(status.getResponseTime());
        assertTrue(status.getResponseTime() >= 0);
        assertNotNull(status.getMessage());
    }
    
    /**
     * 断言查询结果
     */
    public static void assertQueryResult(Map<String, Object> result, boolean expectedSuccess) {
        assertNotNull(result);
        assertEquals(expectedSuccess, result.get("success"));
        
        if (expectedSuccess) {
            assertNotNull(result.get("data"));
            assertNotNull(result.get("count"));
            assertNotNull(result.get("executionTime"));
            assertTrue(((Long) result.get("executionTime")) >= 0);
        } else {
            assertNotNull(result.get("errorMessage"));
        }
    }
    
    /**
     * 断言查询结果包含预期数据
     */
    @SuppressWarnings("unchecked")
    public static void assertQueryResultData(Map<String, Object> result, int expectedRowCount) {
        assertQueryResult(result, true);
        
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        assertEquals(expectedRowCount, data.size());
        assertEquals(expectedRowCount, ((Number) result.get("count")).intValue());
    }
    
    /**
     * 断言表信息列表
     */
    public static void assertTableInfoList(List<DataSourceConnection.TableInfo> tables, int minExpectedCount) {
        assertNotNull(tables);
        assertTrue(tables.size() >= minExpectedCount);
        
        for (DataSourceConnection.TableInfo table : tables) {
            assertNotNull(table.getName());
            assertFalse(table.getName().trim().isEmpty());
            assertNotNull(table.getType());
            // comment可能为null，这是正常的
        }
    }
    
    /**
     * 断言表结构信息
     */
    public static void assertTableSchema(DataSourceConnection.TableSchema schema, String expectedTableName, int minColumnCount) {
        assertNotNull(schema);
        assertEquals(expectedTableName.toUpperCase(), schema.getTableName().toUpperCase());
        
        List<DataSourceConnection.ColumnInfo> columns = schema.getColumns();
        assertNotNull(columns);
        assertTrue(columns.size() >= minColumnCount);
        
        for (DataSourceConnection.ColumnInfo column : columns) {
            assertNotNull(column.getName());
            assertFalse(column.getName().trim().isEmpty());
            assertNotNull(column.getType());
            assertFalse(column.getType().trim().isEmpty());
            // nullable, comment, defaultValue 可能为null或任意值
        }
        
        List<DataSourceConnection.IndexInfo> indexes = schema.getIndexes();
        assertNotNull(indexes);
        // 至少应该有主键索引
        assertTrue(indexes.size() > 0);
        
        for (DataSourceConnection.IndexInfo index : indexes) {
            assertNotNull(index.getName());
            assertNotNull(index.getType());
            assertNotNull(index.getColumns());
            assertFalse(index.getColumns().isEmpty());
        }
    }
    
    /**
     * 断言列信息列表
     */
    public static void assertColumnInfoList(List<DataSourceConnection.ColumnInfo> columns, String... expectedColumnNames) {
        assertNotNull(columns);
        assertTrue(columns.size() >= expectedColumnNames.length);
        
        for (String expectedName : expectedColumnNames) {
            boolean found = columns.stream()
                    .anyMatch(col -> expectedName.equalsIgnoreCase(col.getName()));
            assertTrue(found, "应该包含列: " + expectedName);
        }
    }
    
    /**
     * 断言索引信息列表
     */
    public static void assertIndexInfoList(List<DataSourceConnection.IndexInfo> indexes, int minExpectedCount) {
        assertNotNull(indexes);
        assertTrue(indexes.size() >= minExpectedCount);
        
        for (DataSourceConnection.IndexInfo index : indexes) {
            assertNotNull(index.getName());
            assertNotNull(index.getType());
            assertNotNull(index.getColumns());
            assertTrue(index.getColumns().size() > 0);
            
            for (String column : index.getColumns()) {
                assertNotNull(column);
                assertFalse(column.trim().isEmpty());
            }
        }
    }
    
    /**
     * 断言连接信息
     */
    public static void assertConnectionInfo(DataSourceConnection.ConnectionInfo connectionInfo) {
        assertNotNull(connectionInfo);
        assertNotNull(connectionInfo.getUrl());
        assertFalse(connectionInfo.getUrl().trim().isEmpty());
        assertNotNull(connectionInfo.getVersion());
        assertNotNull(connectionInfo.getProperties());
    }
    
    /**
     * 断言依赖信息
     */
    public static void assertDependencyInfo(DataSourceResponseV2.DependencyInfo dependencyInfo) {
        assertNotNull(dependencyInfo);
        assertNotNull(dependencyInfo.getCoordinate());
        assertFalse(dependencyInfo.getCoordinate().trim().isEmpty());
        assertNotNull(dependencyInfo.getRecommendedVersion());
        assertNotNull(dependencyInfo.getSupportedVersions());
        assertFalse(dependencyInfo.getSupportedVersions().isEmpty());
        // available可能是true或false
        // installCommand可能为null或有值
    }
    
    /**
     * 断言执行时间合理
     */
    public static void assertReasonableExecutionTime(long executionTime, long maxExpectedMs) {
        assertTrue(executionTime >= 0, "执行时间不能为负数");
        assertTrue(executionTime <= maxExpectedMs, 
                String.format("执行时间过长: %dms (期望 <= %dms)", executionTime, maxExpectedMs));
    }
    
    /**
     * 断言列表包含指定元素
     */
    public static <T> void assertListContains(List<T> list, T expectedElement) {
        assertNotNull(list);
        assertTrue(list.contains(expectedElement), 
                "列表应该包含元素: " + expectedElement);
    }
    
    /**
     * 断言字符串包含指定内容（忽略大小写）
     */
    public static void assertContainsIgnoreCase(String actual, String expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertTrue(actual.toLowerCase().contains(expected.toLowerCase()),
                String.format("字符串 '%s' 应该包含 '%s'", actual, expected));
    }
}