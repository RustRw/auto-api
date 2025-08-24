package org.duqiu.fly.autoapi.service.util;

import org.duqiu.fly.autoapi.api.dto.*;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.duqiu.fly.autoapi.api.model.ApiServiceVersion;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.duqiu.fly.autoapi.test.dto.ApiTestResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API服务测试断言工具类
 */
public class ApiServiceTestAssertions {
    
    /**
     * 断言API服务响应与请求匹配
     */
    public static void assertApiServiceResponse(ApiServiceResponse response, ApiServiceCreateRequest request) {
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getPath(), response.getPath());
        assertEquals(request.getMethod(), response.getMethod());
        assertEquals(request.getDataSourceId(), response.getDataSourceId());
        assertEquals(request.getSqlContent(), response.getSqlContent());
        assertEquals(request.getRequestParams(), response.getRequestParams());
        assertEquals(request.getCacheEnabled(), response.getCacheEnabled());
        assertEquals(request.getCacheDuration(), response.getCacheDuration());
        assertEquals(request.getRateLimit(), response.getRateLimit());
        assertEquals(ApiStatus.DRAFT, response.getStatus());
        assertTrue(response.getEnabled());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }
    
    /**
     * 断言API服务实体与请求匹配
     */
    public static void assertApiServiceEntity(ApiService entity, ApiServiceCreateRequest request, Long userId) {
        assertNotNull(entity);
        assertEquals(request.getName(), entity.getName());
        assertEquals(request.getDescription(), entity.getDescription());
        assertEquals(request.getPath(), entity.getPath());
        assertEquals(request.getMethod(), entity.getMethod());
        assertEquals(request.getDataSourceId(), entity.getDataSourceId());
        assertEquals(request.getSqlContent(), entity.getSqlContent());
        assertEquals(request.getRequestParams(), entity.getRequestParams());
        assertEquals(ApiStatus.DRAFT, entity.getStatus());
        assertEquals(userId, entity.getCreatedBy());
        assertEquals(userId, entity.getUpdatedBy());
        assertTrue(entity.getEnabled());
    }
    
    /**
     * 断言API服务更新
     */
    public static void assertApiServiceUpdated(ApiServiceResponse response, ApiServiceUpdateRequest request) {
        assertNotNull(response);
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getDescription(), response.getDescription());
        assertEquals(request.getPath(), response.getPath());
        assertEquals(request.getMethod(), response.getMethod());
        assertEquals(request.getSqlContent(), response.getSqlContent());
        assertEquals(request.getRequestParams(), response.getRequestParams());
        
        if (request.getCacheEnabled() != null) {
            assertEquals(request.getCacheEnabled(), response.getCacheEnabled());
        }
        if (request.getCacheDuration() != null) {
            assertEquals(request.getCacheDuration(), response.getCacheDuration());
        }
        if (request.getRateLimit() != null) {
            assertEquals(request.getRateLimit(), response.getRateLimit());
        }
    }
    
    /**
     * 断言版本响应
     */
    public static void assertVersionResponse(ApiServiceVersionResponse response, ApiService apiService, String version) {
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(apiService.getId(), response.getApiServiceId());
        assertEquals(version, response.getVersion());
        assertEquals(apiService.getName(), response.getName());
        assertEquals(apiService.getDescription(), response.getDescription());
        assertEquals(apiService.getPath(), response.getPath());
        assertEquals(apiService.getMethod(), response.getMethod());
        assertEquals(apiService.getDataSourceId(), response.getDataSourceId());
        assertEquals(apiService.getSqlContent(), response.getSqlContent());
        assertEquals(ApiStatus.PUBLISHED, response.getStatus());
        assertTrue(response.getIsActive());
        assertNotNull(response.getPublishedAt());
        assertNull(response.getUnpublishedAt());
    }
    
    /**
     * 断言版本对比响应
     */
    public static void assertVersionCompareResponse(ApiServiceVersionCompareResponse response,
                                                  String sourceVersion, String targetVersion) {
        assertNotNull(response);
        assertNotNull(response.getSourceVersion());
        assertNotNull(response.getTargetVersion());
        assertEquals(sourceVersion, response.getSourceVersion().getVersion());
        assertEquals(targetVersion, response.getTargetVersion().getVersion());
        assertNotNull(response.getDifferences());
        assertFalse(response.getDifferences().isEmpty());
    }
    
    /**
     * 断言版本差异列表
     */
    public static void assertVersionDifferences(List<ApiServiceVersionCompareResponse.VersionDifference> differences,
                                              String fieldName, boolean shouldHaveDifference) {
        ApiServiceVersionCompareResponse.VersionDifference diff = differences.stream()
                .filter(d -> d.getFieldName().equals(fieldName))
                .findFirst()
                .orElse(null);
        
        assertNotNull(diff, "应该包含字段差异: " + fieldName);
        
        if (shouldHaveDifference) {
            assertNotEquals(ApiServiceVersionCompareResponse.DifferenceType.UNCHANGED, diff.getDifferenceType());
        }
    }
    
    /**
     * 断言测试响应成功
     */
    public static void assertTestResponseSuccess(ApiTestResponse response) {
        assertNotNull(response);
        assertTrue(response.isSuccess(), "测试应该成功");
        assertNotNull(response.getData());
        assertNull(response.getErrorMessage());
        assertNotNull(response.getExecutionTimeMs());
        assertTrue(response.getExecutionTimeMs() >= 0);
        assertNotNull(response.getRecordCount());
        assertTrue(response.getRecordCount() >= 0);
    }
    
    /**
     * 断言测试响应失败
     */
    public static void assertTestResponseFailure(ApiTestResponse response, String expectedError) {
        assertNotNull(response);
        assertFalse(response.isSuccess(), "测试应该失败");
        assertNotNull(response.getErrorMessage());
        if (expectedError != null) {
            assertTrue(response.getErrorMessage().contains(expectedError),
                    "错误信息应该包含: " + expectedError);
        }
        assertNotNull(response.getExecutionTimeMs());
        assertTrue(response.getExecutionTimeMs() >= 0);
        assertEquals(0, response.getRecordCount());
    }
    
    /**
     * 断言SQL执行详情
     */
    public static void assertSqlExecutionDetail(ApiTestResponse.SqlExecutionDetail detail) {
        assertNotNull(detail);
        assertNotNull(detail.getExecutedSql());
        assertFalse(detail.getExecutedSql().trim().isEmpty());
        assertNotNull(detail.getConnectionTimeMs());
        assertTrue(detail.getConnectionTimeMs() >= 0);
        assertNotNull(detail.getQueryTimeMs());
        assertTrue(detail.getQueryTimeMs() >= 0);
        assertNotNull(detail.getProcessingTimeMs());
        assertTrue(detail.getProcessingTimeMs() >= 0);
    }
    
    /**
     * 断言审计日志
     */
    public static void assertAuditLog(ApiServiceAuditLog log, Long apiServiceId, 
                                    ApiServiceAuditLog.OperationType operationType, Long userId) {
        assertNotNull(log);
        assertEquals(apiServiceId, log.getApiServiceId());
        assertEquals(operationType, log.getOperationType());
        assertEquals(userId, log.getCreatedBy());
        assertNotNull(log.getOperationDescription());
        assertFalse(log.getOperationDescription().trim().isEmpty());
        assertNotNull(log.getOperationResult());
        assertNotNull(log.getDurationMs());
        assertTrue(log.getDurationMs() >= 0);
    }
    
    /**
     * 断言审计日志成功
     */
    public static void assertAuditLogSuccess(ApiServiceAuditLog log) {
        assertEquals(ApiServiceAuditLog.OperationResult.SUCCESS, log.getOperationResult());
        assertNull(log.getErrorMessage());
    }
    
    /**
     * 断言审计日志失败
     */
    public static void assertAuditLogFailure(ApiServiceAuditLog log, String expectedError) {
        assertEquals(ApiServiceAuditLog.OperationResult.FAILED, log.getOperationResult());
        assertNotNull(log.getErrorMessage());
        if (expectedError != null) {
            assertTrue(log.getErrorMessage().contains(expectedError));
        }
    }
    
    /**
     * 断言表选择配置
     */
    public static void assertTableSelection(TableSelectionRequest actual, TableSelectionRequest expected) {
        assertNotNull(actual);
        assertEquals(expected.getTableName(), actual.getTableName());
        assertEquals(expected.getTableAlias(), actual.getTableAlias());
        assertEquals(expected.getIsPrimary(), actual.getIsPrimary());
        assertEquals(expected.getJoinType(), actual.getJoinType());
        assertEquals(expected.getJoinCondition(), actual.getJoinCondition());
        assertEquals(expected.getSortOrder(), actual.getSortOrder());
        
        if (expected.getSelectedColumns() != null) {
            assertNotNull(actual.getSelectedColumns());
            assertEquals(expected.getSelectedColumns().size(), actual.getSelectedColumns().size());
            assertTrue(actual.getSelectedColumns().containsAll(expected.getSelectedColumns()));
        }
    }
    
    /**
     * 断言SQL模板生成
     */
    public static void assertSqlTemplate(String sqlTemplate, List<TableSelectionRequest> tableSelections) {
        assertNotNull(sqlTemplate);
        assertFalse(sqlTemplate.trim().isEmpty());
        assertTrue(sqlTemplate.toUpperCase().contains("SELECT"), "应该包含SELECT关键字");
        assertTrue(sqlTemplate.toUpperCase().contains("FROM"), "应该包含FROM关键字");
        
        // 验证主表
        TableSelectionRequest primaryTable = tableSelections.stream()
                .filter(t -> t.getIsPrimary())
                .findFirst()
                .orElse(tableSelections.get(0));
        assertTrue(sqlTemplate.contains(primaryTable.getTableName()), "应该包含主表名");
        
        // 验证JOIN语句
        long joinTableCount = tableSelections.stream()
                .filter(t -> !t.getIsPrimary())
                .count();
        if (joinTableCount > 0) {
            assertTrue(sqlTemplate.toUpperCase().contains("JOIN"), "应该包含JOIN关键字");
        }
    }
    
    /**
     * 断言时间范围合理
     */
    public static void assertReasonableTimeRange(LocalDateTime start, LocalDateTime end, long maxDurationSeconds) {
        assertNotNull(start);
        assertNotNull(end);
        assertTrue(end.isAfter(start) || end.isEqual(start));
        
        if (maxDurationSeconds > 0) {
            long actualDuration = java.time.Duration.between(start, end).getSeconds();
            assertTrue(actualDuration <= maxDurationSeconds, 
                    String.format("时间间隔过长: %d秒 (最大允许: %d秒)", actualDuration, maxDurationSeconds));
        }
    }
    
    /**
     * 断言列表不为空且包含期望元素
     */
    public static <T> void assertListContainsAll(List<T> actualList, List<T> expectedElements) {
        assertNotNull(actualList);
        assertFalse(actualList.isEmpty());
        
        for (T expected : expectedElements) {
            assertTrue(actualList.contains(expected), 
                    "列表应该包含元素: " + expected);
        }
    }
    
    /**
     * 断言字符串包含所有期望的子字符串
     */
    public static void assertStringContainsAll(String actual, String... expectedSubstrings) {
        assertNotNull(actual);
        
        for (String expected : expectedSubstrings) {
            assertTrue(actual.contains(expected), 
                    String.format("字符串 '%s' 应该包含 '%s'", actual, expected));
        }
    }
}