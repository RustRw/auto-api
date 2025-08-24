package org.duqiu.fly.autoapi.controller;

import jakarta.validation.Valid;
import org.duqiu.fly.autoapi.api.dto.ApiServiceCreateRequest;
import org.duqiu.fly.autoapi.api.dto.ApiServiceResponse;
import org.duqiu.fly.autoapi.api.service.ApiServiceManager;
import org.duqiu.fly.autoapi.common.dto.PageResult;
import org.duqiu.fly.autoapi.common.dto.Result;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * API服务管理控制器 - Web Gateway层
 */
@RestController
@RequestMapping("/api/services")
public class ApiServiceController {
    
    private final ApiServiceManager apiServiceService;
    
    public ApiServiceController(ApiServiceManager apiServiceService) {
        this.apiServiceService = apiServiceService;
    }
    
    @PostMapping
    public Result<ApiServiceResponse> createApiService(
            @Valid @RequestBody ApiServiceCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            ApiServiceResponse response = apiServiceService.createApiService(request, userId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("创建API服务失败: " + e.getMessage());
        }
    }
    
    @GetMapping
    public Result<PageResult<ApiServiceResponse>> getUserApiServices(
            Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            PageResult<ApiServiceResponse> result = apiServiceService.getUserApiServices(userId, pageable);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("获取API服务列表失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public Result<ApiServiceResponse> getApiService(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            ApiServiceResponse response = apiServiceService.getApiServiceById(id, userId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("获取API服务失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public Result<ApiServiceResponse> updateApiService(
            @PathVariable Long id,
            @Valid @RequestBody ApiServiceCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            ApiServiceResponse response = apiServiceService.updateApiService(id, request, userId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("更新API服务失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/status")
    public Result<ApiServiceResponse> updateApiServiceStatus(
            @PathVariable Long id,
            @RequestParam ApiStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            ApiServiceResponse response = apiServiceService.updateApiServiceStatus(id, status, userId);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("更新API服务状态失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteApiService(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = 1L; // TODO: 从认证信息中获取真实用户ID
            apiServiceService.deleteApiService(id, userId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除API服务失败: " + e.getMessage());
        }
    }
}