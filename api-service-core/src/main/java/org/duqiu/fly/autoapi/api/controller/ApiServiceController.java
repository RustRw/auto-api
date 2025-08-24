package org.duqiu.fly.autoapi.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.duqiu.fly.autoapi.api.dto.*;
import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.duqiu.fly.autoapi.api.service.ApiServiceExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API服务管理控制器
 */
@RestController("coreApiServiceController")
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ApiServiceController {
    
    private final ApiServiceExecutor apiServiceExecutor;
    
    /**
     * 创建API服务
     */
    @PostMapping
    public ResponseEntity<ApiServiceResponse> createApiService(
            @Valid @RequestBody ApiServiceCreateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ApiServiceResponse response = apiServiceExecutor.createApiService(request, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 更新API服务
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiServiceResponse> updateApiService(
            @PathVariable Long id,
            @Valid @RequestBody ApiServiceUpdateRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ApiServiceResponse response = apiServiceExecutor.updateApiService(id, request, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 发布API服务
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiServiceVersionResponse> publishApiService(
            @PathVariable Long id,
            @Valid @RequestBody ApiServicePublishRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ApiServiceVersionResponse response = apiServiceExecutor.publishApiService(id, request, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 下线API服务
     */
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<Void> unpublishApiService(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        apiServiceExecutor.unpublishApiService(id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 删除API服务
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApiService(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        apiServiceExecutor.deleteApiService(id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取API服务版本列表
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<Page<ApiServiceVersionResponse>> getVersions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiServiceVersionResponse> versions = apiServiceExecutor.getVersions(id, userId, pageable);
        return ResponseEntity.ok(versions);
    }
    
    /**
     * 版本对比
     */
    @GetMapping("/{id}/versions/compare")
    public ResponseEntity<ApiServiceVersionCompareResponse> compareVersions(
            @PathVariable Long id,
            @RequestParam String sourceVersion,
            @RequestParam String targetVersion,
            @RequestHeader("X-User-Id") Long userId) {
        ApiServiceVersionCompareResponse response = 
                apiServiceExecutor.compareVersions(id, sourceVersion, targetVersion, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取操作日志
     */
    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<Page<ApiServiceAuditLog>> getAuditLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApiServiceAuditLog> logs = apiServiceExecutor.getAuditLogs(id, userId, pageable);
        return ResponseEntity.ok(logs);
    }
}