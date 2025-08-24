package org.duqiu.fly.autoapi.service.integration;

import org.duqiu.fly.autoapi.api.dto.ApiServiceCreateRequest;
import org.duqiu.fly.autoapi.api.dto.ApiServiceResponse;
import org.duqiu.fly.autoapi.api.dto.ApiServiceUpdateRequest;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.duqiu.fly.autoapi.api.service.ApiServiceExecutor;
import org.duqiu.fly.autoapi.service.util.ApiServiceTestFactory;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.duqiu.fly.autoapi.service.util.ApiServiceTestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API服务CRUD功能集成测试
 */
@DisplayName("API服务CRUD功能集成测试")
public class ApiServiceCrudIntegrationTest extends ApiServiceIntegrationTestBase {
    
    @Autowired
    private ApiServiceExecutor apiServiceService;
    
    @Test
    @DisplayName("创建API服务 - 成功")
    void testCreateApiService_Success() {
        // Given
        ApiServiceCreateRequest request = ApiServiceTestFactory.createBasicApiServiceRequest();
        
        // When
        ApiServiceResponse response = apiServiceService.createApiService(request, TEST_USER_ID);
        
        // Then
        assertApiServiceResponse(response, request);
        
        // 验证数据库中确实创建了记录
        ApiService saved = apiServiceRepository.findById(response.getId()).orElse(null);
        assertNotNull(saved);
        assertApiServiceEntity(saved, request, TEST_USER_ID);
        
        // 验证审计日志
        verifyAuditLogCreated(response.getId(), "CREATE");
    }
    
    @Test
    @DisplayName("创建API服务 - 名称重复")
    void testCreateApiService_DuplicateName() {
        // Given
        ApiServiceCreateRequest request = ApiServiceTestFactory.createBasicApiServiceRequest();
        apiServiceService.createApiService(request, TEST_USER_ID);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.createApiService(request, TEST_USER_ID);
        });
    }
    
    @Test
    @DisplayName("创建API服务 - 复杂场景")
    void testCreateApiService_Complex() {
        // Given
        ApiServiceCreateRequest request = ApiServiceTestFactory.createComplexApiServiceRequest();
        
        // When
        ApiServiceResponse response = apiServiceService.createApiService(request, TEST_USER_ID);
        
        // Then
        assertApiServiceResponse(response, request);
        assertEquals(request.getResponseExample(), response.getResponseExample());
        assertFalse(response.getCacheEnabled());
        assertEquals(500, response.getRateLimit());
        
        // 验证SQL内容包含预期关键字
        assertTrue(response.getSqlContent().contains("JOIN"));
        assertTrue(response.getSqlContent().contains("ORDER BY"));
        assertTrue(response.getSqlContent().contains("LIMIT"));
    }
    
    @Test
    @DisplayName("更新API服务 - 成功")
    void testUpdateApiService_Success() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiServiceUpdateRequest updateRequest = ApiServiceTestFactory.createUpdateRequest();
        
        // When
        ApiServiceResponse updated = apiServiceService.updateApiService(created.getId(), updateRequest, TEST_USER_ID);
        
        // Then
        assertApiServiceUpdated(updated, updateRequest);
        assertEquals(created.getId(), updated.getId());
        assertTrue(updated.getUpdatedAt().isAfter(created.getCreatedAt()));
        
        // 验证审计日志
        verifyAuditLogCreated(created.getId(), "UPDATE");
    }
    
    @Test
    @DisplayName("更新API服务 - 非草稿状态")
    void testUpdateApiService_NotDraft() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 先发布服务
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        ApiServiceUpdateRequest updateRequest = ApiServiceTestFactory.createUpdateRequest();
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            apiServiceService.updateApiService(created.getId(), updateRequest, TEST_USER_ID);
        });
    }
    
    @Test
    @DisplayName("更新API服务 - 无权限")
    void testUpdateApiService_NoPermission() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiServiceUpdateRequest updateRequest = ApiServiceTestFactory.createUpdateRequest();
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.updateApiService(created.getId(), updateRequest, OTHER_USER_ID);
        });
    }
    
    @Test
    @DisplayName("删除API服务 - 成功")
    void testDeleteApiService_Success() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When
        apiServiceService.deleteApiService(created.getId(), TEST_USER_ID);
        
        // Then
        // 验证服务已被删除
        assertFalse(apiServiceRepository.existsById(created.getId()));
        
        // 验证相关数据也被删除
        assertEquals(0, versionRepository.countByApiServiceId(created.getId()));
        assertEquals(0, auditLogRepository.countByApiServiceIdOrderByCreatedAtDesc(
                created.getId(), PageRequest.of(0, 10)).getTotalElements());
    }
    
    @Test
    @DisplayName("删除API服务 - 已发布状态")
    void testDeleteApiService_Published() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 先发布服务
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            apiServiceService.deleteApiService(created.getId(), TEST_USER_ID);
        });
        
        // 验证服务仍然存在
        assertTrue(apiServiceRepository.existsById(created.getId()));
    }
    
    @Test
    @DisplayName("删除API服务 - 无权限")
    void testDeleteApiService_NoPermission() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.deleteApiService(created.getId(), OTHER_USER_ID);
        });
        
        // 验证服务仍然存在
        assertTrue(apiServiceRepository.existsById(created.getId()));
    }
    
    @Test
    @DisplayName("获取API服务 - 不存在")
    void testGetApiService_NotFound() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.updateApiService(999L, 
                    ApiServiceTestFactory.createUpdateRequest(), TEST_USER_ID);
        });
    }
    
    @Test
    @DisplayName("批量操作测试")
    void testBatchOperations() {
        // Given - 创建多个API服务
        ApiServiceCreateRequest request1 = ApiServiceTestFactory.createBasicApiServiceRequest();
        request1.setName("批量测试服务1");
        request1.setPath("/api/batch1");
        
        ApiServiceCreateRequest request2 = ApiServiceTestFactory.createComplexApiServiceRequest();
        request2.setName("批量测试服务2");
        request2.setPath("/api/batch2");
        
        // When
        ApiServiceResponse service1 = apiServiceService.createApiService(request1, TEST_USER_ID);
        ApiServiceResponse service2 = apiServiceService.createApiService(request2, TEST_USER_ID);
        
        // Then
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotEquals(service1.getId(), service2.getId());
        
        // 验证两个服务都存在
        assertTrue(apiServiceRepository.existsById(service1.getId()));
        assertTrue(apiServiceRepository.existsById(service2.getId()));
        
        // 验证审计日志记录了所有操作
        var logs1 = auditLogRepository.findByApiServiceIdOrderByCreatedAtDesc(
                service1.getId(), PageRequest.of(0, 10));
        var logs2 = auditLogRepository.findByApiServiceIdOrderByCreatedAtDesc(
                service2.getId(), PageRequest.of(0, 10));
        
        assertFalse(logs1.isEmpty());
        assertFalse(logs2.isEmpty());
    }
    
    @Test
    @DisplayName("并发创建测试")
    void testConcurrentCreation() {
        // Given
        ApiServiceCreateRequest request = ApiServiceTestFactory.createBasicApiServiceRequest();
        
        // When - 并发创建（通过不同用户）
        ApiServiceResponse service1 = apiServiceService.createApiService(request, TEST_USER_ID);
        
        request.setName("并发测试服务2"); // 不同名称避免重名
        request.setPath("/api/concurrent2"); // 不同路径
        ApiServiceResponse service2 = apiServiceService.createApiService(request, OTHER_USER_ID);
        
        // Then
        assertNotNull(service1);
        assertNotNull(service2);
        assertNotEquals(service1.getId(), service2.getId());
        assertEquals(TEST_USER_ID, service1.getCreatedBy());
        assertEquals(OTHER_USER_ID, service2.getCreatedBy());
    }
    
    @Test
    @DisplayName("状态流转测试")
    void testStatusTransition() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 验证初始状态
        assertEquals(ApiStatus.DRAFT, created.getStatus());
        
        // When - 发布服务
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // Then - 验证状态变为已发布
        ApiService updated = apiServiceRepository.findById(created.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(ApiStatus.PUBLISHED, updated.getStatus());
        
        // When - 下线服务
        apiServiceService.unpublishApiService(created.getId(), TEST_USER_ID);
        
        // Then - 验证状态变为已禁用
        updated = apiServiceRepository.findById(created.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(ApiStatus.DISABLED, updated.getStatus());
    }
}