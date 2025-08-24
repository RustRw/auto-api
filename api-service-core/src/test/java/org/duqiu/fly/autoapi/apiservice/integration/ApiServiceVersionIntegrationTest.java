package org.duqiu.fly.autoapi.service.integration;

import org.duqiu.fly.autoapi.api.dto.*;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.model.ApiServiceVersion;
import org.duqiu.fly.autoapi.api.service.EnhancedApiServiceService;
import org.duqiu.fly.autoapi.service.util.ApiServiceTestFactory;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.duqiu.fly.autoapi.service.util.ApiServiceTestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * API服务版本管理集成测试
 */
@DisplayName("API服务版本管理集成测试")
public class ApiServiceVersionIntegrationTest extends ApiServiceIntegrationTestBase {
    
    @Autowired
    private EnhancedApiServiceService apiServiceService;
    
    @Test
    @DisplayName("发布API服务版本 - 成功")
    void testPublishApiService_Success() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        ApiServicePublishRequest publishRequest = ApiServiceTestFactory.createPublishRequest("1.0.0");
        
        // When
        ApiServiceVersionResponse versionResponse = apiServiceService.publishApiService(
                created.getId(), publishRequest, TEST_USER_ID);
        
        // Then
        assertNotNull(versionResponse);
        assertEquals("1.0.0", versionResponse.getVersion());
        assertEquals(created.getId(), versionResponse.getApiServiceId());
        assertEquals(ApiStatus.PUBLISHED, versionResponse.getStatus());
        assertTrue(versionResponse.getIsActive());
        assertNotNull(versionResponse.getPublishedAt());
        assertNull(versionResponse.getUnpublishedAt());
        
        // 验证版本快照数据
        assertVersionResponse(versionResponse, apiServiceRepository.findById(created.getId()).orElse(null), "1.0.0");
        
        // 验证原服务状态已更新
        ApiService updated = apiServiceRepository.findById(created.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(ApiStatus.PUBLISHED, updated.getStatus());
        
        // 验证审计日志
        verifyAuditLogCreated(created.getId(), "PUBLISH");
    }
    
    @Test
    @DisplayName("发布API服务版本 - 版本号重复")
    void testPublishApiService_DuplicateVersion() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 先发布一个版本
        ApiServicePublishRequest publishRequest = ApiServiceTestFactory.createPublishRequest("1.0.0");
        apiServiceService.publishApiService(created.getId(), publishRequest, TEST_USER_ID);
        
        // When & Then - 尝试发布相同版本号
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.publishApiService(created.getId(), publishRequest, TEST_USER_ID);
        });
    }
    
    @Test
    @DisplayName("发布API服务版本 - 强制发布")
    void testPublishApiService_ForcePublish() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 先发布一个版本
        ApiServicePublishRequest publishRequest = ApiServiceTestFactory.createPublishRequest("1.0.0");
        ApiServiceVersionResponse version1 = apiServiceService.publishApiService(
                created.getId(), publishRequest, TEST_USER_ID);
        
        // 修改服务内容
        ApiServiceUpdateRequest updateRequest = ApiServiceTestFactory.createUpdateRequest();
        updateRequest.setSqlContent("SELECT id, username FROM test_users WHERE status = ${status}");
        
        // 临时改为草稿状态以便更新
        ApiService service = apiServiceRepository.findById(created.getId()).orElse(null);
        service.setStatus(ApiStatus.DRAFT);
        apiServiceRepository.save(service);
        
        apiServiceService.updateApiService(created.getId(), updateRequest, TEST_USER_ID);
        
        // When - 强制发布相同版本号
        ApiServicePublishRequest forcePublishRequest = ApiServiceTestFactory.createForcePublishRequest("1.0.0");
        ApiServiceVersionResponse version2 = apiServiceService.publishApiService(
                created.getId(), forcePublishRequest, TEST_USER_ID);
        
        // Then
        assertNotNull(version2);
        assertEquals("1.0.0", version2.getVersion());
        assertTrue(version2.getIsActive());
        
        // 验证新版本包含更新的SQL
        assertTrue(version2.getSqlContent().contains("SELECT id, username"));
        
        // 验证原版本不再活跃
        ApiServiceVersion oldVersion = versionRepository.findById(version1.getId()).orElse(null);
        assertNotNull(oldVersion);
        assertFalse(oldVersion.getIsActive());
    }
    
    @Test
    @DisplayName("发布多个版本")
    void testPublishMultipleVersions() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When - 发布多个版本
        ApiServiceVersionResponse v1 = apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // 为了发布新版本，先改为草稿状态
        ApiService service = apiServiceRepository.findById(created.getId()).orElse(null);
        service.setStatus(ApiStatus.DRAFT);
        service.setSqlContent("SELECT * FROM test_users WHERE id = ${id}");
        apiServiceRepository.save(service);
        
        ApiServiceVersionResponse v2 = apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.1.0"), TEST_USER_ID);
        
        service = apiServiceRepository.findById(created.getId()).orElse(null);
        service.setStatus(ApiStatus.DRAFT);
        service.setSqlContent("SELECT id, username, email FROM test_users WHERE id = ${id}");
        apiServiceRepository.save(service);
        
        ApiServiceVersionResponse v3 = apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("2.0.0"), TEST_USER_ID);
        
        // Then
        assertEquals("1.0.0", v1.getVersion());
        assertEquals("1.1.0", v2.getVersion());
        assertEquals("2.0.0", v3.getVersion());
        
        // 验证只有最新版本是活跃的
        assertFalse(versionRepository.findById(v1.getId()).orElse(null).getIsActive());
        assertFalse(versionRepository.findById(v2.getId()).orElse(null).getIsActive());
        assertTrue(versionRepository.findById(v3.getId()).orElse(null).getIsActive());
        
        // 验证版本总数
        assertEquals(3, versionRepository.countByApiServiceId(created.getId()));
    }
    
    @Test
    @DisplayName("获取版本列表")
    void testGetVersions() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 创建多个版本
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        ApiService service = apiServiceRepository.findById(created.getId()).orElse(null);
        service.setStatus(ApiStatus.DRAFT);
        apiServiceRepository.save(service);
        
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.1.0"), TEST_USER_ID);
        
        // When
        Page<ApiServiceVersionResponse> versions = apiServiceService.getVersions(
                created.getId(), TEST_USER_ID, PageRequest.of(0, 10));
        
        // Then
        assertNotNull(versions);
        assertEquals(2, versions.getTotalElements());
        assertEquals(2, versions.getContent().size());
        
        // 验证版本按时间倒序排列
        List<ApiServiceVersionResponse> versionList = versions.getContent();
        assertEquals("1.1.0", versionList.get(0).getVersion()); // 最新版本在前
        assertEquals("1.0.0", versionList.get(1).getVersion());
        
        // 验证活跃版本
        assertTrue(versionList.get(0).getIsActive());
        assertFalse(versionList.get(1).getIsActive());
    }
    
    @Test
    @DisplayName("版本对比 - 基本对比")
    void testCompareVersions_Basic() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 发布第一个版本
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // 修改并发布第二个版本
        ApiService service = apiServiceRepository.findById(created.getId()).orElse(null);
        service.setStatus(ApiStatus.DRAFT);
        service.setName("用户详情查询服务V2");
        service.setSqlContent("SELECT id, username, email FROM test_users WHERE status = ${status}");
        service.setDescription("增强版用户查询服务");
        apiServiceRepository.save(service);
        
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("2.0.0"), TEST_USER_ID);
        
        // When
        ApiServiceVersionCompareResponse compareResponse = apiServiceService.compareVersions(
                created.getId(), "1.0.0", "2.0.0", TEST_USER_ID);
        
        // Then
        assertVersionCompareResponse(compareResponse, "1.0.0", "2.0.0");
        
        List<ApiServiceVersionCompareResponse.VersionDifference> differences = compareResponse.getDifferences();
        assertFalse(differences.isEmpty());
        
        // 验证具体差异
        assertVersionDifferences(differences, "name", true);
        assertVersionDifferences(differences, "sqlContent", true);
        assertVersionDifferences(differences, "description", true);
        
        // 验证未变化的字段
        assertVersionDifferences(differences, "method", false);
        assertVersionDifferences(differences, "path", false);
    }
    
    @Test
    @DisplayName("版本对比 - 相同版本")
    void testCompareVersions_SameVersion() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // When
        ApiServiceVersionCompareResponse compareResponse = apiServiceService.compareVersions(
                created.getId(), "1.0.0", "1.0.0", TEST_USER_ID);
        
        // Then
        assertVersionCompareResponse(compareResponse, "1.0.0", "1.0.0");
        
        // 所有字段都应该是未变化
        List<ApiServiceVersionCompareResponse.VersionDifference> differences = compareResponse.getDifferences();
        long unchangedCount = differences.stream()
                .mapToLong(diff -> diff.getDifferenceType() == ApiServiceVersionCompareResponse.DifferenceType.UNCHANGED ? 1 : 0)
                .sum();
        
        assertTrue(unchangedCount > 0, "应该有未变化的字段");
    }
    
    @Test
    @DisplayName("版本对比 - 版本不存在")
    void testCompareVersions_VersionNotFound() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.compareVersions(created.getId(), "1.0.0", "2.0.0", TEST_USER_ID);
        });
    }
    
    @Test
    @DisplayName("下线API服务")
    void testUnpublishApiService() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // 发布版本
        ApiServiceVersionResponse published = apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // When
        apiServiceService.unpublishApiService(created.getId(), TEST_USER_ID);
        
        // Then
        // 验证服务状态变为禁用
        ApiService service = apiServiceRepository.findById(created.getId()).orElse(null);
        assertNotNull(service);
        assertEquals(ApiStatus.DISABLED, service.getStatus());
        
        // 验证版本状态更新
        ApiServiceVersion version = versionRepository.findById(published.getId()).orElse(null);
        assertNotNull(version);
        assertFalse(version.getIsActive());
        assertNotNull(version.getUnpublishedAt());
        
        // 验证审计日志
        verifyAuditLogCreated(created.getId(), "UNPUBLISH");
    }
    
    @Test
    @DisplayName("下线API服务 - 非发布状态")
    void testUnpublishApiService_NotPublished() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When & Then - 尝试下线草稿状态的服务
        assertThrows(IllegalStateException.class, () -> {
            apiServiceService.unpublishApiService(created.getId(), TEST_USER_ID);
        });
    }
    
    @Test
    @DisplayName("版本权限测试")
    void testVersionPermissions() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createBasicApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When & Then - 其他用户尝试发布版本
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.publishApiService(created.getId(), 
                    ApiServiceTestFactory.createPublishRequest("1.0.0"), OTHER_USER_ID);
        });
        
        // When & Then - 其他用户尝试获取版本列表
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.getVersions(created.getId(), OTHER_USER_ID, PageRequest.of(0, 10));
        });
        
        // When & Then - 其他用户尝试版本对比
        apiServiceService.publishApiService(created.getId(), 
                ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        assertThrows(IllegalArgumentException.class, () -> {
            apiServiceService.compareVersions(created.getId(), "1.0.0", "1.0.0", OTHER_USER_ID);
        });
    }
    
    @Test
    @DisplayName("版本快照完整性测试")
    void testVersionSnapshotIntegrity() {
        // Given
        ApiServiceCreateRequest createRequest = ApiServiceTestFactory.createComplexApiServiceRequest();
        ApiServiceResponse created = apiServiceService.createApiService(createRequest, TEST_USER_ID);
        
        // When
        ApiServiceVersionResponse version = apiServiceService.publishApiService(
                created.getId(), ApiServiceTestFactory.createPublishRequest("1.0.0"), TEST_USER_ID);
        
        // Then - 验证版本快照包含所有原始数据
        assertEquals(created.getName(), version.getName());
        assertEquals(created.getDescription(), version.getDescription());
        assertEquals(created.getPath(), version.getPath());
        assertEquals(created.getMethod(), version.getMethod());
        assertEquals(created.getDataSourceId(), version.getDataSourceId());
        assertEquals(created.getSqlContent(), version.getSqlContent());
        assertEquals(created.getRequestParams(), version.getRequestParams());
        assertEquals(created.getCacheEnabled(), version.getCacheEnabled());
        assertEquals(created.getCacheDuration(), version.getCacheDuration());
        assertEquals(created.getRateLimit(), version.getRateLimit());
        
        // 修改原服务后，版本快照不应该受影响
        ApiService service = apiServiceRepository.findById(created.getId()).orElse(null);
        service.setStatus(ApiStatus.DRAFT);
        service.setName("修改后的服务名称");
        service.setSqlContent("SELECT * FROM modified_table");
        apiServiceRepository.save(service);
        
        // 重新获取版本，验证快照数据未变化
        ApiServiceVersion versionEntity = versionRepository.findById(version.getId()).orElse(null);
        assertNotNull(versionEntity);
        assertEquals(created.getName(), versionEntity.getName()); // 仍然是原始名称
        assertEquals(created.getSqlContent(), versionEntity.getSqlContent()); // 仍然是原始SQL
    }
}