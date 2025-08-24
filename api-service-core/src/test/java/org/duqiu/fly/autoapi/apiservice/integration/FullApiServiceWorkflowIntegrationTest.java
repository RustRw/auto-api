package org.duqiu.fly.autoapi.service.integration;

import org.duqiu.fly.autoapi.api.dto.*;
import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.duqiu.fly.autoapi.api.service.EnhancedApiServiceService;
import org.duqiu.fly.autoapi.api.service.TableSelectionService;
import org.duqiu.fly.autoapi.service.util.ApiServiceTestFactory;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.dto.ApiTestResponse;
import org.duqiu.fly.autoapi.test.service.EnhancedApiTestingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

import static org.duqiu.fly.autoapi.service.util.ApiServiceTestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 完整的API服务工作流集成测试
 * 模拟从创建API服务到最终部署和使用的完整流程
 */
@DisplayName("完整API服务工作流集成测试")
public class FullApiServiceWorkflowIntegrationTest extends ApiServiceIntegrationTestBase {
    
    @Autowired
    private EnhancedApiServiceService apiServiceService;
    
    @Autowired
    private EnhancedApiTestingService testingService;
    
    @Autowired
    private TableSelectionService tableSelectionService;
    
    @Test
    @DisplayName("完整工作流测试 - 用户查询API")
    void testCompleteWorkflow_UserQueryApi() {
        // Phase 1: 创建API服务
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        createRequest.setName("用户信息查询API");
        createRequest.setDescription("根据用户状态查询用户基本信息");
        createRequest.setPath("/api/v1/users");
        
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        assertEquals(ApiStatus.DRAFT, created.getStatus());
        assertNotNull(created.getId());
        
        // Phase 2: 配置表选择
        List<TableSelectionRequest> tableSelections = ApiServiceTestFactory.createTableSelectionRequests();
        tableSelectionService.saveTableSelections(created.getId(), tableSelections, TEST_USER_ID);
        
        // 验证表选择配置
        List<TableSelectionRequest> savedSelections = tableSelectionService.getTableSelections(created.getId());
        assertEquals(2, savedSelections.size());
        
        // Phase 3: 生成和优化SQL
        String sqlTemplate = tableSelectionService.generateSqlTemplate(created.getId());
        assertNotNull(sqlTemplate);
        assertTrue(sqlTemplate.contains("LEFT JOIN"));
        
        // 更新API服务的SQL
        ApiServiceUpdateRequest updateRequest = new ApiServiceUpdateRequest();
        updateRequest.setName(created.getName());
        updateRequest.setDescription("优化后的用户查询API，添加了订单统计");
        updateRequest.setPath(created.getPath());
        updateRequest.setMethod(created.getMethod());
        updateRequest.setSqlContent(
            "SELECT u.id, u.username, u.email, u.status, " +
            "COUNT(o.id) as order_count, SUM(o.total_price) as total_spent " +
            "FROM test_users u " +
            "LEFT JOIN test_orders o ON u.id = o.user_id " +
            "WHERE u.status = ${status} " +
            "GROUP BY u.id, u.username, u.email, u.status " +
            "ORDER BY u.created_at DESC"
        );
        updateRequest.setUpdateDescription("添加了订单统计功能");
        
        ApiServiceResponse updated = apiServiceService.updateApiService(
                created.getId(), updateRequest, TEST_USER_ID);
        
        assertTrue(updated.getSqlContent().contains("COUNT"));
        assertTrue(updated.getSqlContent().contains("SUM"));
        
        // Phase 4: 测试API功能
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(Map.of("status", "ACTIVE"));
        
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        assertTestResponseSuccess(testResponse);
        assertTrue(testResponse.getRecordCount() > 0);
        
        // 验证返回数据包含统计字段
        assertNotNull(testResponse.getSqlExecutionDetail());
        String executedSql = testResponse.getSqlExecutionDetail().getExecutedSql();
        assertTrue(executedSql.contains("COUNT(o.id)"));
        assertTrue(executedSql.contains("'ACTIVE'"));
        
        // Phase 5: 发布第一个版本
        ApiServicePublishRequest publishRequest = ApiServiceTestFactory.createPublishRequest("1.0.0");
        publishRequest.setVersionDescription("初始版本，包含基本用户查询和订单统计功能");
        
        ApiServiceVersionResponse version1 = apiServiceService.publishApiService(
                created.getId(), publishRequest, TEST_USER_ID);
        
        assertEquals("1.0.0", version1.getVersion());
        assertEquals(ApiStatus.PUBLISHED, version1.getStatus());
        assertTrue(version1.getIsActive());
        
        // 验证服务状态已更新
        var service = apiServiceRepository.findById(created.getId()).orElse(null);
        assertEquals(ApiStatus.PUBLISHED, service.getStatus());
        
        // Phase 6: 测试已发布版本
        ApiTestResponse publishedTestResponse = testingService.testPublishedApi(
                created.getId(), "1.0.0", Map.of("status", "ACTIVE"), TEST_USER_ID);
        
        assertTestResponseSuccess(publishedTestResponse);
        assertEquals(testResponse.getRecordCount(), publishedTestResponse.getRecordCount());
        
        // Phase 7: 功能迭代 - 添加新功能
        service.setStatus(ApiStatus.DRAFT); // 临时改为草稿状态以便修改
        service.setSqlContent(
            updateRequest.getSqlContent() + " HAVING total_spent > ${minSpent}"
        );
        service.setDescription("V2版本，增加了消费金额过滤");
        apiServiceRepository.save(service);
        
        // 测试新功能
        testRequest.setParameters(Map.of("status", "ACTIVE", "minSpent", "1000"));
        ApiTestResponse v2TestResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        assertTestResponseSuccess(v2TestResponse);
        assertTrue(v2TestResponse.getSqlExecutionDetail().getExecutedSql().contains("HAVING"));
        assertTrue(v2TestResponse.getSqlExecutionDetail().getExecutedSql().contains("1000"));
        
        // Phase 8: 发布第二个版本
        ApiServiceVersionResponse version2 = apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("2.0.0"), TEST_USER_ID);
        
        assertEquals("2.0.0", version2.getVersion());
        assertTrue(version2.getIsActive());
        assertFalse(versionRepository.findById(version1.getId()).orElse(null).getIsActive());
        
        // Phase 9: 版本对比
        ApiServiceVersionCompareResponse compareResponse = apiServiceService.compareVersions(
                created.getId(), "1.0.0", "2.0.0", TEST_USER_ID);
        
        assertVersionCompareResponse(compareResponse, "1.0.0", "2.0.0");
        
        List<ApiServiceVersionCompareResponse.VersionDifference> differences = compareResponse.getDifferences();
        boolean hasSqlDifference = differences.stream()
                .anyMatch(diff -> diff.getFieldName().equals("sqlContent") && 
                         diff.getDifferenceType() == ApiServiceVersionCompareResponse.DifferenceType.MODIFIED);
        assertTrue(hasSqlDifference);
        
        // Phase 10: 测试不同版本
        ApiTestResponse v1Response = testingService.testPublishedApi(
                created.getId(), "1.0.0", Map.of("status", "ACTIVE"), TEST_USER_ID);
        ApiTestResponse v2Response = testingService.testPublishedApi(
                created.getId(), "2.0.0", Map.of("status", "ACTIVE", "minSpent", "1000"), TEST_USER_ID);
        
        assertTestResponseSuccess(v1Response);
        assertTestResponseSuccess(v2Response);
        
        // V2版本应该返回更少的记录（因为有消费金额过滤）
        assertTrue(v2Response.getRecordCount() <= v1Response.getRecordCount());
        
        // Phase 11: 验证审计日志完整性
        Page<ApiServiceAuditLog> auditLogs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 20));
        
        assertTrue(auditLogs.getTotalElements() >= 6); // 至少包含：创建、更新、测试、发布v1、发布v2、版本对比
        
        // 验证不同类型的操作都被记录
        List<ApiServiceAuditLog> logs = auditLogs.getContent();
        boolean hasCreate = logs.stream().anyMatch(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.CREATE);
        boolean hasUpdate = logs.stream().anyMatch(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.UPDATE);
        boolean hasTest = logs.stream().anyMatch(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.TEST);
        boolean hasPublish = logs.stream().anyMatch(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.PUBLISH);
        boolean hasCompare = logs.stream().anyMatch(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.VERSION_COMPARE);
        
        assertTrue(hasCreate);
        assertTrue(hasUpdate);
        assertTrue(hasTest);
        assertTrue(hasPublish);
        assertTrue(hasCompare);
        
        // Phase 12: 服务下线
        apiServiceService.unpublishApiService(created.getId(), TEST_USER_ID);
        
        service = apiServiceRepository.findById(created.getId()).orElse(null);
        assertEquals(ApiStatus.DISABLED, service.getStatus());
        
        var currentVersion = versionRepository.findActiveVersionByApiServiceId(created.getId());
        assertTrue(currentVersion.isEmpty() || !currentVersion.get().getIsActive());
        
        // 验证下线操作被记录
        auditLogs = apiServiceService.getAuditLogs(created.getId(), TEST_USER_ID, PageRequest.of(0, 20));
        boolean hasUnpublish = auditLogs.getContent().stream()
                .anyMatch(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.UNPUBLISH);
        assertTrue(hasUnpublish);
    }
    
    @Test
    @DisplayName("复杂业务场景测试 - 订单分析API")
    void testComplexBusinessScenario_OrderAnalysisApi() {
        // Phase 1: 创建复杂的订单分析API
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createComplexApiServiceRequest();
        createRequest.setName("订单分析API");
        createRequest.setDescription("多维度订单数据分析");
        createRequest.setPath("/api/v1/analytics/orders");
        createRequest.setSqlContent(
            "SELECT " +
            "    u.username, " +
            "    p.name as product_name, " +
            "    c.name as category_name, " +
            "    o.quantity, " +
            "    o.unit_price, " +
            "    o.total_price, " +
            "    o.status, " +
            "    o.order_date, " +
            "    CASE " +
            "        WHEN o.total_price > 5000 THEN 'HIGH' " +
            "        WHEN o.total_price > 1000 THEN 'MEDIUM' " +
            "        ELSE 'LOW' " +
            "    END as price_level " +
            "FROM test_orders o " +
            "INNER JOIN test_users u ON o.user_id = u.id " +
            "INNER JOIN test_products p ON o.product_id = p.id " +
            "INNER JOIN test_categories c ON p.category_id = c.id " +
            "WHERE o.status = ${orderStatus} " +
            "AND u.status = ${userStatus} " +
            "AND o.order_date >= ${startDate} " +
            "AND o.total_price >= ${minAmount} " +
            "ORDER BY o.order_date DESC, o.total_price DESC " +
            "LIMIT ${limit}"
        );
        
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // Phase 2: 复杂参数测试
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(Map.of(
            "orderStatus", "COMPLETED",
            "userStatus", "ACTIVE",
            "startDate", "2024-01-01",
            "minAmount", "100",
            "limit", "10"
        ));
        
        ApiTestResponse testResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        assertTestResponseSuccess(testResponse);
        
        // 验证复杂SQL执行
        String executedSql = testResponse.getSqlExecutionDetail().getExecutedSql();
        assertStringContainsAll(executedSql,
            "CASE WHEN", "INNER JOIN", "test_users", "test_products", "test_categories",
            "'COMPLETED'", "'ACTIVE'", "'2024-01-01'", "100", "10"
        );
        
        // Phase 3: 边界条件测试
        Map<String, Object> edgeCaseParams = Map.of(
            "orderStatus", "PENDING",
            "userStatus", "ACTIVE",
            "startDate", "2024-12-31",
            "minAmount", "0",
            "limit", "1"
        );
        
        testRequest.setParameters(edgeCaseParams);
        ApiTestResponse edgeTestResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        assertTestResponseSuccess(edgeTestResponse);
        assertTrue(edgeTestResponse.getRecordCount() <= 1);
        
        // Phase 4: 性能测试
        Map<String, Object> performanceParams = Map.of(
            "orderStatus", "COMPLETED",
            "userStatus", "ACTIVE", 
            "startDate", "2020-01-01",
            "minAmount", "0",
            "limit", "1000"
        );
        
        testRequest.setParameters(performanceParams);
        long startTime = System.currentTimeMillis();
        ApiTestResponse perfTestResponse = testingService.testDraftApi(testRequest, TEST_USER_ID);
        long duration = System.currentTimeMillis() - startTime;
        
        assertTestResponseSuccess(perfTestResponse);
        assertTrue(duration < 5000); // 应该在5秒内完成
        assertTrue(perfTestResponse.getExecutionTimeMs() < 3000); // SQL执行应该在3秒内
        
        // Phase 5: 发布和版本管理
        ApiServiceVersionResponse version = apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        assertNotNull(version);
        assertEquals(ApiStatus.PUBLISHED, version.getStatus());
        
        // Phase 6: 批量测试不同参数组合
        List<Map<String, Object>> batchParams = List.of(
            Map.of("orderStatus", "COMPLETED", "userStatus", "ACTIVE", "startDate", "2024-01-01", "minAmount", "0", "limit", "5"),
            Map.of("orderStatus", "PENDING", "userStatus", "ACTIVE", "startDate", "2024-01-01", "minAmount", "1000", "limit", "10"),
            Map.of("orderStatus", "CANCELLED", "userStatus", "ACTIVE", "startDate", "2024-01-01", "minAmount", "0", "limit", "20")
        );
        
        List<ApiTestResponse> batchResults = testingService.batchTestApi(
                created.getId(), batchParams, TEST_USER_ID);
        
        assertEquals(3, batchResults.size());
        
        for (int i = 0; i < batchResults.size(); i++) {
            ApiTestResponse result = batchResults.get(i);
            if (result.getSuccess()) {
                assertTestResponseSuccess(result);
                assertEquals(batchParams.get(i), result.getTestParameters());
            }
        }
        
        // Phase 7: 验证完整的审计追踪
        Page<ApiServiceAuditLog> finalAuditLogs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 50));
        
        // 应该包含多次测试操作的记录
        long testCount = finalAuditLogs.getContent().stream()
                .mapToLong(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.TEST ? 1 : 0)
                .sum();
        
        assertTrue(testCount >= 4); // 至少4次测试：基本测试、边界测试、性能测试、批量测试
        
        // 验证所有测试操作都成功
        boolean allTestsSuccessful = finalAuditLogs.getContent().stream()
                .filter(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.TEST)
                .allMatch(log -> log.getOperationResult() == ApiServiceAuditLog.OperationResult.SUCCESS);
        
        assertTrue(allTestsSuccessful);
    }
    
    @Test
    @DisplayName("多用户协作场景测试")
    void testMultiUserCollaborationScenario() {
        // User 1 创建API服务
        ApiServiceCreateRequest request1 = ApiServiceTestFactory.createBasicApiServiceRequest();
        request1.setName("用户1的API服务");
        request1.setPath("/api/user1/service");
        
        ApiServiceResponse service1 = apiServiceService.createApiService(request1, TEST_USER_ID);
        
        // User 2 创建相似的API服务
        ApiServiceCreateRequest request2 = ApiServiceTestFactory.createBasicApiServiceRequest();
        request2.setName("用户2的API服务");
        request2.setPath("/api/user2/service");
        
        ApiServiceResponse service2 = apiServiceService.createApiService(request2, OTHER_USER_ID);
        
        // 验证用户隔离
        assertNotEquals(service1.getId(), service2.getId());
        assertEquals(TEST_USER_ID, service1.getCreatedBy());
        assertEquals(OTHER_USER_ID, service2.getCreatedBy());
        
        // User 1 无法访问 User 2 的服务
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.updateApiService(service2.getId(), 
                    ApiServiceTestFactory.createUpdateRequest(), TEST_USER_ID);
        });
        
        // User 2 无法访问 User 1 的服务
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.getAuditLogs(service1.getId(), OTHER_USER_ID, PageRequest.of(0, 10));
        });
        
        // 各自可以正常操作自己的服务
        apiServiceService.publishApiService(service1.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        apiServiceService.publishApiService(service2.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), OTHER_USER_ID);
        
        // 验证版本隔离
        Page<ApiServiceVersionResponse> user1Versions = apiServiceService.getVersions(
                service1.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        Page<ApiServiceVersionResponse> user2Versions = apiServiceService.getVersions(
                service2.getId(), OTHER_USER_ID, PageRequest.of(0, 10));
        
        assertEquals(1, user1Versions.getTotalElements());
        assertEquals(1, user2Versions.getTotalElements());
        
        assertEquals(service1.getId(), user1Versions.getContent().get(0).getApiServiceId());
        assertEquals(service2.getId(), user2Versions.getContent().get(0).getApiServiceId());
    }
}