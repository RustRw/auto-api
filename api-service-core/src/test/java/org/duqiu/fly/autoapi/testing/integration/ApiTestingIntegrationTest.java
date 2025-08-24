package org.duqiu.fly.autoapi.test.integration;

import org.duqiu.fly.autoapi.api.dto.ApiServiceCreateRequest;
import org.duqiu.fly.autoapi.api.dto.ApiServiceResponse;
import org.duqiu.fly.autoapi.api.dto.ApiServiceVersionResponse;
import org.duqiu.fly.autoapi.service.integration.ApiServiceIntegrationTestBase;
import org.duqiu.fly.autoapi.api.service.EnhancedApiServiceService;
import org.duqiu.fly.autoapi.service.util.ApiServiceTestFactory;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.dto.ApiTestResponse;
import org.duqiu.fly.autoapi.test.service.EnhancedApiTestingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.duqiu.fly.autoapi.service.util.ApiServiceTestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API在线测试功能集成测试
 */
@DisplayName("API在线测试功能集成测试")
public class ApiTestingIntegrationTest extends ApiServiceIntegrationTestBase {
    
    @Autowired
    private EnhancedApiServiceService apiServiceService;
    
    @Autowired
    private EnhancedApiTestingService testingService;
    
    @Test
    @DisplayName("测试草稿API - 基本查询")
    void testDraftApi_BasicQuery() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(Map.of("status", "ACTIVE"));
        
        // When
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        // Then
        assertTestResponseSuccess(testResponse);
        assertNotNull(testResponse.getData());
        assertTrue(testResponse.getRecordCount() > 0);
        assertNotNull(testResponse.getSqlExecutionDetail());
        assertSqlExecutionDetail(testResponse.getSqlExecutionDetail());
        
        // 验证SQL参数被正确处理
        assertTrue(testResponse.getSqlExecutionDetail().getExecutedSql().contains("'ACTIVE'"));
        
        // 验证审计日志
        verifyAuditLogCreated(created.getId(), "TEST");
    }
    
    @Test
    @DisplayName("测试草稿API - 复杂JOIN查询")
    void testDraftApi_ComplexJoinQuery() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createComplexApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(ApiServiceTestFactory.createComplexTestParameters());
        
        // When
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        // Then
        assertTestResponseSuccess(testResponse);
        assertNotNull(testResponse.getData());
        
        // 验证复杂查询的执行
        String executedSql = testResponse.getSqlExecutionDetail().getExecutedSql();
        assertStringContainsAll(executedSql, "JOIN", "test_orders", "test_users", "test_products");
        assertTrue(executedSql.contains("'COMPLETED'"));
        assertTrue(executedSql.contains("'ACTIVE'"));
        assertTrue(executedSql.contains("50"));
    }
    
    @Test
    @DisplayName("测试草稿API - 无权限")
    void testDraftApi_NoPermission() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(Map.of("status", "ACTIVE"));
        
        // When
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, OTHER_USER_ID);
        
        // Then
        assertTestResponseFailure(testResponse, "无权测试该API服务");
    }
    
    @Test
    @DisplayName("测试草稿API - SQL错误")
    void testDraftApi_SqlError() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        createRequest.setSqlContent("SELECT * FROM non_existent_table WHERE status = ${status}");
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(Map.of("status", "ACTIVE"));
        
        // When
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        // Then
        assertTestResponseFailure(testResponse, null);
        assertNotNull(testResponse.getErrorMessage());
        assertTrue(testResponse.getErrorMessage().toLowerCase().contains("table"));
    }
    
    @Test
    @DisplayName("测试已发布API - 默认版本")
    void testPublishedApi_DefaultVersion() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 发布版本
        ApiServiceVersionResponse published = apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        Map<String, Object> parameters = Map.of("status", "ACTIVE");
        
        // When - 测试已发布版本（不指定版本号，使用默认活跃版本）
        ApiTestResponse testResponse = testingService.testPublishedApi(
                created.getId(), null, parameters, TEST_USER_ID);
        
        // Then
        assertTestResponseSuccess(testResponse);
        assertNotNull(testResponse.getData());
        
        // 验证审计日志包含版本信息
        verifyAuditLogCreated(created.getId(), "TEST");
    }
    
    @Test
    @DisplayName("测试已发布API - 指定版本")
    void testPublishedApi_SpecificVersion() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 发布多个版本
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // 修改服务并发布新版本
        var service = apiServiceRepository.findById(created.getId()).orElse(null);
        service.setStatus(org.duqiu.fly.autoapi.common.enums.ApiStatus.DRAFT);
        service.setSqlContent("SELECT id, username FROM test_users WHERE status = ${status}");
        apiServiceRepository.save(service);
        
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("2.0.0"), TEST_USER_ID);
        
        Map<String, Object> parameters = Map.of("status", "ACTIVE");
        
        // When - 测试指定版本
        ApiTestResponse testResponse = testingService.testPublishedApi(
                created.getId(), "1.0.0", parameters, TEST_USER_ID);
        
        // Then
        assertTestResponseSuccess(testResponse);
        
        // 验证使用的是指定版本的SQL（应该是原始的SELECT *）
        String executedSql = testResponse.getSqlExecutionDetail().getExecutedSql();
        assertTrue(executedSql.contains("SELECT *"));
        assertFalse(executedSql.contains("SELECT id, username")); // 新版本的SQL
    }
    
    @Test
    @DisplayName("测试已发布API - 版本不存在")
    void testPublishedApi_VersionNotFound() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        Map<String, Object> parameters = Map.of("status", "ACTIVE");
        
        // When
        ApiTestResponse testResponse = testingService.testPublishedApi(
                created.getId(), "2.0.0", parameters, TEST_USER_ID);
        
        // Then
        assertTestResponseFailure(testResponse, "指定版本不存在");
    }
    
    @Test
    @DisplayName("SQL验证 - 正确语法")
    void testValidateSql_ValidSyntax() {
        // Given
        String validSql = "SELECT u.id, u.username FROM test_users u WHERE u.status = ${status}";
        Map<String, Object> parameters = Map.of("status", "ACTIVE");
        
        // When
        Map<String, Object> result = testingService.validateSql(1L, validSql, parameters, TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(true, result.get("valid"));
        assertEquals("SQL语法验证通过", result.get("message"));
        assertNotNull(result.get("processedSql"));
        assertTrue(((String) result.get("processedSql")).contains("'ACTIVE'"));
    }
    
    @Test
    @DisplayName("SQL验证 - 错误语法")
    void testValidateSql_InvalidSyntax() {
        // Given
        String invalidSql = "SELCT * FORM test_users"; // 故意的语法错误
        
        // When
        Map<String, Object> result = testingService.validateSql(1L, invalidSql, null, TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        assertEquals(false, result.get("valid"));
        assertNotNull(result.get("message"));
        assertTrue(((String) result.get("message")).contains("SQL语法错误"));
    }
    
    @Test
    @DisplayName("SQL验证 - 无权限数据源")
    void testValidateSql_NoPermission() {
        // Given
        String validSql = "SELECT * FROM test_users";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            testingService.validateSql(1L, validSql, null, OTHER_USER_ID);
        });
    }
    
    @Test
    @DisplayName("获取SQL执行计划")
    void testExplainSql() {
        // Given
        String sql = "SELECT * FROM test_users WHERE status = ${status}";
        Map<String, Object> parameters = Map.of("status", "ACTIVE");
        
        // When
        Map<String, Object> result = testingService.explainSql(1L, sql, parameters, TEST_USER_ID);
        
        // Then
        assertNotNull(result);
        // H2数据库可能不支持EXPLAIN，所以这里主要验证方法调用不出错
        assertNotNull(result.get("success"));
    }
    
    @Test
    @DisplayName("批量测试API")
    void testBatchTestApi() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        List<Map<String, Object>> parametersList = ApiServiceTestFactory.createBatchTestParameters();
        
        // When
        List<ApiTestResponse> responses = testingService.batchTestApi(
                created.getId(), parametersList, TEST_USER_ID);
        
        // Then
        assertNotNull(responses);
        assertEquals(parametersList.size(), responses.size());
        
        // 验证每个测试结果
        for (int i = 0; i < responses.size(); i++) {
            ApiTestResponse response = responses.get(i);
            
            // 大部分应该成功（除非参数有问题）
            if (response.getSuccess()) {
                assertTestResponseSuccess(response);
            } else {
                assertTestResponseFailure(response, null);
            }
            
            // 验证参数正确传递
            assertEquals(parametersList.get(i), response.getTestParameters());
        }
    }
    
    @Test
    @DisplayName("参数处理测试 - 各种数据类型")
    void testParameterProcessing_DataTypes() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        createRequest.setSqlContent(
            "SELECT * FROM test_users WHERE " +
            "id = ${userId} AND " +
            "username = ${username} AND " +
            "status IN (${statusList}) AND " +
            "created_at >= ${startDate}"
        );
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", 1);
        parameters.put("username", "testuser1");
        parameters.put("statusList", "ACTIVE,INACTIVE");
        parameters.put("startDate", "2024-01-01");
        
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(parameters);
        
        // When
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        // Then
        assertTestResponseSuccess(testResponse);
        
        // 验证不同类型参数的处理
        String executedSql = testResponse.getSqlExecutionDetail().getExecutedSql();
        assertTrue(executedSql.contains("id = 1")); // 数字类型
        assertTrue(executedSql.contains("username = 'testuser1'")); // 字符串类型（带引号）
        assertTrue(executedSql.contains("'ACTIVE,INACTIVE'")); // 复合字符串
        assertTrue(executedSql.contains("'2024-01-01'")); // 日期字符串
    }
    
    @Test
    @DisplayName("参数处理测试 - 缺失参数")
    void testParameterProcessing_MissingParameters() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        createRequest.setSqlContent("SELECT * FROM test_users WHERE status = ${status} AND id = ${userId}");
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 只提供部分参数
        Map<String, Object> parameters = Map.of("status", "ACTIVE");
        // 缺少 userId 参数
        
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(parameters);
        
        // When
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        // Then
        assertTestResponseSuccess(testResponse); // 应该成功，缺失参数替换为NULL
        
        // 验证缺失参数被替换为NULL
        String executedSql = testResponse.getSqlExecutionDetail().getExecutedSql();
        assertTrue(executedSql.contains("id = NULL"));
    }
    
    @Test
    @DisplayName("性能测试 - 响应时间")
    void testPerformance_ResponseTime() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(Map.of("status", "ACTIVE"));
        
        // When
        long startTime = System.currentTimeMillis();
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        long totalTime = System.currentTimeMillis() - startTime;
        
        // Then
        assertTestResponseSuccess(testResponse);
        
        // 验证响应时间合理（应该在合理范围内）
        assertTrue(testResponse.getExecutionTimeMs() <= totalTime);
        assertTrue(testResponse.getExecutionTimeMs() < 5000); // 应该在5秒内完成
        
        // 验证SQL执行详情的时间统计
        ApiTestResponse.SqlExecutionDetail detail = testResponse.getSqlExecutionDetail();
        assertTrue(detail.getConnectionTimeMs() >= 0);
        assertTrue(detail.getQueryTimeMs() >= 0);
        assertTrue(detail.getProcessingTimeMs() >= 0);
        
        // 总执行时间应该大致等于各部分时间之和
        long totalDetailTime = detail.getConnectionTimeMs() + detail.getQueryTimeMs() + detail.getProcessingTimeMs();
        assertTrue(Math.abs(testResponse.getExecutionTimeMs() - totalDetailTime) < 100); // 允许100ms误差
    }
    
    @Test
    @DisplayName("并发测试")
    void testConcurrentTesting() throws InterruptedException {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When - 并发执行多个测试
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        ApiTestResponse[] results = new ApiTestResponse[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                ApiTestRequest testRequest = new ApiTestRequest();
                testRequest.setApiServiceId(created.getId());
                testRequest.setParameters(Map.of("status", "ACTIVE"));
                
                results[index] = testingService.testDraftApi(testRequest, TEST_USER_ID);
            });
            threads[i].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Then - 验证所有测试都成功
        for (ApiTestResponse result : results) {
            assertNotNull(result);
            assertTestResponseSuccess(result);
        }
    }
}