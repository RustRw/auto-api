package org.duqiu.fly.autoapi.api.service;

import lombok.RequiredArgsConstructor;
import org.duqiu.fly.autoapi.api.dto.*;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnhancedApiServiceService {
    
    private final ApiServiceManager apiServiceManager;
    
    public ApiServiceResponse createApiService(ApiServiceCreateRequest request, Long userId) {
        return apiServiceManager.createApiService(request, userId);
    }
    
    public ApiServiceResponse getApiService(Long id, Long userId) {
        return apiServiceManager.getApiServiceById(id, userId);
    }
    
    public void deleteApiService(Long id, Long userId) {
        apiServiceManager.deleteApiService(id, userId);
    }
    
    // Additional methods needed by tests
    public ApiServiceResponse updateApiService(Long id, ApiServiceUpdateRequest request, Long userId) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    public ApiServiceVersionResponse publishApiService(Long id, ApiServicePublishRequest request, Long userId) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    public ApiServiceVersionCompareResponse compareVersions(Long id, String version1, String version2, Long userId) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    public Page<ApiServiceAuditLog> getAuditLogs(Long apiServiceId, Long userId, PageRequest pageRequest) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    public void unpublishApiService(Long id, Long userId) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    public Page<ApiServiceVersionResponse> getVersions(Long apiServiceId, Long userId, PageRequest pageRequest) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
}