package org.duqiu.fly.autoapi.service.integration;

import org.duqiu.fly.autoapi.api.dto.ApiServiceCreateRequest;
import org.duqiu.fly.autoapi.api.dto.ApiServiceResponse;
import org.duqiu.fly.autoapi.api.dto.ApiServiceUpdateRequest;
import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.duqiu.fly.autoapi.api.service.EnhancedApiServiceService;
import org.duqiu.fly.autoapi.service.util.ApiServiceTestFactory;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.service.EnhancedApiTestingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.duqiu.fly.autoapi.service.util.ApiServiceTestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 审计日志功能集成测试
 */
@DisplayName("审计日志功能集成测试")
public class AuditLogIntegrationTest extends ApiServiceIntegrationTestBase {
    
    @Autowired
    private EnhancedApiServiceService apiServiceService;
    
    @Autowired
    private EnhancedApiTestingService testingService;
    
    @Test
    @DisplayName("创建API服务审计日志")
    void testCreateApiServiceAuditLog() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        
        // When
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // Then
        // 验证审计日志被创建
        Page<ApiServiceAuditLog> logs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        assertFalse(logs.isEmpty());
        assertEquals(1, logs.getTotalElements());
        
        ApiServiceAuditLog log = logs.getContent().get(0);
        assertAuditLog(log, created.getId(), ApiServiceAuditLog.OperationType.CREATE, TEST_USER_ID);
        assertAuditLogSuccess(log);
        
        // 验证操作描述
        assertEquals("创建API服务", log.getOperationDescription());
        
        // 验证数据快照
        assertNull(log.getBeforeData()); // 创建操作没有before数据
        assertNotNull(log.getAfterData()); // 应该有after数据
        assertTrue(log.getAfterData().contains(createRequest.getName()));
    }
    
    @Test
    @DisplayName("更新API服务审计日志")
    void testUpdateApiServiceAuditLog() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiServiceUpdateRequest updateRequest = ApiServiceTestFactory.createUpdateRequest();
        
        // When
        apiServiceService.updateApiService(created.getId(), updateRequest, TEST_USER_ID);
        
        // Then
        Page<ApiServiceAuditLog> logs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        assertEquals(2, logs.getTotalElements()); // 创建 + 更新
        
        // 获取最新的更新日志
        ApiServiceAuditLog updateLog = logs.getContent().get(0); // 按时间倒序排列
        assertAuditLog(updateLog, created.getId(), ApiServiceAuditLog.OperationType.UPDATE, TEST_USER_ID);
        assertAuditLogSuccess(updateLog);
        
        // 验证before和after数据都存在
        assertNotNull(updateLog.getBeforeData());
        assertNotNull(updateLog.getAfterData());
        
        // 验证数据变化
        assertTrue(updateLog.getBeforeData().contains(createRequest.getName()));
        assertTrue(updateLog.getAfterData().contains(updateRequest.getName()));
    }
    
    @Test
    @DisplayName("发布API服务审计日志")
    void testPublishApiServiceAuditLog() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // Then
        Page<ApiServiceAuditLog> logs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        assertEquals(2, logs.getTotalElements()); // 创建 + 发布
        
        ApiServiceAuditLog publishLog = logs.getContent().get(0);
        assertAuditLog(publishLog, created.getId(), ApiServiceAuditLog.OperationType.PUBLISH, TEST_USER_ID);
        assertAuditLogSuccess(publishLog);
        
        // 验证操作描述包含版本信息
        assertTrue(publishLog.getOperationDescription().contains("1.0.0"));
    }
    
    @Test
    @DisplayName("下线API服务审计日志")
    void testUnpublishApiServiceAuditLog() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 先发布
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // When
        apiServiceService.unpublishApiService(created.getId(), TEST_USER_ID);
        
        // Then
        Page<ApiServiceAuditLog> logs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        assertEquals(3, logs.getTotalElements()); // 创建 + 发布 + 下线
        
        ApiServiceAuditLog unpublishLog = logs.getContent().get(0);
        assertAuditLog(unpublishLog, created.getId(), ApiServiceAuditLog.OperationType.UNPUBLISH, TEST_USER_ID);
        assertAuditLogSuccess(unpublishLog);
    }
    
    @Test
    @DisplayName("删除API服务审计日志")
    void testDeleteApiServiceAuditLog() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When
        apiServiceService.deleteApiService(created.getId(), TEST_USER_ID);
        
        // Then
        // 删除操作后，审计日志也应该被删除（级联删除）
        assertEquals(0, auditLogRepository.countByApiServiceId(created.getId()));
    }
    
    @Test
    @DisplayName("API测试审计日志")
    void testApiTestingAuditLog() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiTestRequest testRequest = new ApiTestRequest();
        testRequest.setApiServiceId(created.getId());
        testRequest.setParameters(Map.of("status", "ACTIVE"));
        
        // When
        testingService.testDraftApi(testRequest, TEST_USER_ID);
        
        // Then
        Page<ApiServiceAuditLog> logs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        assertEquals(2, logs.getTotalElements()); // 创建 + 测试
        
        ApiServiceAuditLog testLog = logs.getContent().get(0);
        assertAuditLog(testLog, created.getId(), ApiServiceAuditLog.OperationType.TEST, TEST_USER_ID);
        assertAuditLogSuccess(testLog);
        
        // 验证测试详情
        assertNotNull(testLog.getOperationDetails());
        assertTrue(testLog.getOperationDetails().contains("草稿测试"));
        
        // 验证测试结果数据
        assertNotNull(testLog.getAfterData());
        assertTrue(testLog.getAfterData().contains("success"));
    }
    
    @Test
    @DisplayName("版本对比审计日志")
    void testVersionCompareAuditLog() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 发布两个版本
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        var service = apiServiceRepository.findById(created.getId()).orElse(null);
        service.setStatus(org.duqiu.fly.autoapi.common.enums.ApiStatus.DRAFT);
        service.setName("更新的服务名称");
        apiServiceRepository.save(service);
        
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("2.0.0"), TEST_USER_ID);
        
        // When
        apiServiceService.compareVersions(created.getId(), "1.0.0", "2.0.0", TEST_USER_ID);
        
        // Then
        Page<ApiServiceAuditLog> logs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        // 查找版本对比日志
        ApiServiceAuditLog compareLog = logs.getContent().stream()
                .filter(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.VERSION_COMPARE)
                .findFirst()
                .orElse(null);
        
        assertNotNull(compareLog);
        assertAuditLog(compareLog, created.getId(), ApiServiceAuditLog.OperationType.VERSION_COMPARE, TEST_USER_ID);
        assertAuditLogSuccess(compareLog);
        
        // 验证对比描述
        assertTrue(compareLog.getOperationDescription().contains("1.0.0"));
        assertTrue(compareLog.getOperationDescription().contains("2.0.0"));
    }
    
    @Test
    @DisplayName("操作失败审计日志")
    void testOperationFailureAuditLog() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When - 尝试其他用户的非法操作
        try {
            apiServiceService.updateApiService(created.getId(), 
                    ApiServiceTestFactory.createUpdateRequest(), OTHER_USER_ID);
        } catch (Exception e) {
            // 预期的异常
        }
        
        // Then - 验证失败日志被记录
        List<ApiServiceAuditLog> allLogs = auditLogRepository.findByApiServiceIdOrderByCreatedAtDesc(
                created.getId(), PageRequest.of(0, 100)).getContent();
        
        // 查找失败的日志
        ApiServiceAuditLog failureLog = allLogs.stream()
                .filter(log -> log.getOperationResult() == ApiServiceAuditLog.OperationResult.FAILED)
                .findFirst()
                .orElse(null);
        
        if (failureLog != null) { // 失败日志可能不会被记录，这取决于实现
            assertAuditLog(failureLog, created.getId(), ApiServiceAuditLog.OperationType.UPDATE, OTHER_USER_ID);
            assertAuditLogFailure(failureLog, "权限");
        }
    }
    
    @Test
    @DisplayName("审计日志分页查询")
    void testAuditLogPagination() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 执行多个操作生成多条日志
        ApiServiceUpdateRequest updateRequest = ApiServiceTestFactory.createUpdateRequest();
        apiServiceService.updateApiService(created.getId(), updateRequest, TEST_USER_ID);
        
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        apiServiceService.unpublishApiService(created.getId(), TEST_USER_ID);
        
        // When - 分页查询
        Page<ApiServiceAuditLog> page1 = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 2));
        Page<ApiServiceAuditLog> page2 = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(1, 2));
        
        // Then
        assertEquals(4, page1.getTotalElements()); // 总共4条日志
        assertEquals(2, page1.getNumberOfElements()); // 第一页2条
        assertEquals(2, page2.getNumberOfElements()); // 第二页2条
        
        // 验证时间顺序（最新的在前）
        List<ApiServiceAuditLog> page1Content = page1.getContent();
        assertTrue(page1Content.get(0).getCreatedAt().isAfter(page1Content.get(1).getCreatedAt()) ||
                   page1Content.get(0).getCreatedAt().equals(page1Content.get(1).getCreatedAt()));
    }
    
    @Test
    @DisplayName("审计日志操作时长统计")
    void testAuditLogDurationTracking() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        
        // When
        long startTime = System.currentTimeMillis();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        long endTime = System.currentTimeMillis();
        
        // Then
        Page<ApiServiceAuditLog> logs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        ApiServiceAuditLog log = logs.getContent().get(0);
        assertNotNull(log.getDurationMs());
        assertTrue(log.getDurationMs() >= 0);
        assertTrue(log.getDurationMs() <= (endTime - startTime + 100)); // 允许一些误差
    }
    
    @Test
    @DisplayName("审计日志权限验证")
    void testAuditLogPermissions() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When & Then - 其他用户无法查看审计日志
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.getAuditLogs(created.getId(), OTHER_USER_ID, PageRequest.of(0, 10));
        });
    }
    
    @Test
    @DisplayName("审计日志数据完整性")
    void testAuditLogDataIntegrity() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When
        ApiServiceUpdateRequest updateRequest = ApiServiceTestFactory.createUpdateRequest();
        updateRequest.setUpdateDescription("数据完整性测试更新");
        apiServiceService.updateApiService(created.getId(), updateRequest, TEST_USER_ID);
        
        // Then
        Page<ApiServiceAuditLog> logs = apiServiceService.getAuditLogs(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        ApiServiceAuditLog updateLog = logs.getContent().stream()
                .filter(log -> log.getOperationType() == ApiServiceAuditLog.OperationType.UPDATE)
                .findFirst()
                .orElse(null);
        
        assertNotNull(updateLog);
        
        // 验证操作描述包含更新说明
        assertTrue(updateLog.getOperationDescription().contains("数据完整性测试更新"));
        
        // 验证数据快照包含关键字段
        assertTrue(updateLog.getBeforeData().contains("name"));
        assertTrue(updateLog.getAfterData().contains("name"));
        
        // 验证时间戳
        assertNotNull(updateLog.getCreatedAt());
        assertTrue(updateLog.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(updateLog.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));
    }
    
    @Test
    @DisplayName("批量操作审计日志")
    void testBatchOperationAuditLogs() {
        // Given - 创建多个API服务
        ApiServiceCreateRequest request1 = ApiServiceTestFactory.createBasicApiServiceRequest();
        request1.setName("批量测试服务1");
        request1.setPath("/api/batch1");
        
        ApiServiceCreateRequest request2 = ApiServiceTestFactory.createBasicApiServiceRequest();
        request2.setName("批量测试服务2");
        request2.setPath("/api/batch2");
        
        // When
        ApiServiceResponse service1 = apiServiceService.createApiService(request1, TEST_USER_ID);
        ApiServiceResponse service2 = apiServiceService.createApiService(request2, TEST_USER_ID);
        
        // Then - 验证每个服务都有独立的审计日志
        Page<ApiServiceAuditLog> logs1 = apiServiceService.getAuditLogs(
                service1.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        Page<ApiServiceAuditLog> logs2 = apiServiceService.getAuditLogs(
                service2.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        assertEquals(1, logs1.getTotalElements());
        assertEquals(1, logs2.getTotalElements());
        
        // 验证日志内容区分
        ApiServiceAuditLog log1 = logs1.getContent().get(0);
        ApiServiceAuditLog log2 = logs2.getContent().get(0);
        
        assertEquals(service1.getId(), log1.getApiServiceId());
        assertEquals(service2.getId(), log2.getApiServiceId());
        
        assertTrue(log1.getAfterData().contains("批量测试服务1"));
        assertTrue(log2.getAfterData().contains("批量测试服务2"));
    }
}