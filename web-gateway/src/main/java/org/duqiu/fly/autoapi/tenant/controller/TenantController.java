package org.duqiu.fly.autoapi.tenant.controller;

import org.duqiu.fly.autoapi.common.context.TenantContext;
import org.duqiu.fly.autoapi.common.dto.Result;
import org.duqiu.fly.autoapi.common.model.Tenant;
import org.duqiu.fly.autoapi.tenant.dto.CreateTenantRequest;
import org.duqiu.fly.autoapi.tenant.dto.TenantInfoResponse;
import org.duqiu.fly.autoapi.tenant.service.TenantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 租户管理控制器
 */
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    
    private final TenantService tenantService;
    
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }
    
    /**
     * 创建租户 (仅系统管理员)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<TenantInfoResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        try {
            Long currentUserId = TenantContext.getUserId();
            if (currentUserId == null) {
                return Result.error("用户未认证");
            }
            
            Tenant tenant = tenantService.createTenant(
                request.getTenantCode(),
                request.getTenantName(), 
                request.getDescription(),
                request.getContactEmail(),
                currentUserId
            );
            
            TenantInfoResponse response = convertToResponse(tenant);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("创建租户失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取当前租户信息
     */
    @GetMapping("/current")
    public Result<TenantInfoResponse> getCurrentTenantInfo() {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Result.error("租户信息缺失");
            }
            
            // 这里应该通过TenantService获取租户信息
            // 为了简化，暂时返回基本信息
            TenantInfoResponse response = new TenantInfoResponse();
            response.setTenantId(tenantId);
            response.setTenantCode(TenantContext.getTenantCode());
            
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("获取租户信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 暂停租户 (仅系统管理员)
     */
    @PostMapping("/{tenantId}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> suspendTenant(@PathVariable Long tenantId) {
        try {
            Long currentUserId = TenantContext.getUserId();
            if (currentUserId == null) {
                return Result.error("用户未认证");
            }
            
            tenantService.suspendTenant(tenantId, currentUserId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("暂停租户失败: " + e.getMessage());
        }
    }
    
    /**
     * 激活租户 (仅系统管理员)
     */
    @PostMapping("/{tenantId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> activateTenant(@PathVariable Long tenantId) {
        try {
            Long currentUserId = TenantContext.getUserId();
            if (currentUserId == null) {
                return Result.error("用户未认证");
            }
            
            tenantService.activateTenant(tenantId, currentUserId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("激活租户失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取租户用户数量统计
     */
    @GetMapping("/stats/users")
    public Result<Long> getTenantUserCount() {
        try {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                return Result.error("租户信息缺失");
            }
            
            long userCount = tenantService.getTenantUserCount(tenantId);
            return Result.success(userCount);
        } catch (Exception e) {
            return Result.error("获取用户统计失败: " + e.getMessage());
        }
    }
    
    private TenantInfoResponse convertToResponse(Tenant tenant) {
        TenantInfoResponse response = new TenantInfoResponse();
        response.setTenantId(tenant.getId());
        response.setTenantCode(tenant.getTenantCode());
        response.setTenantName(tenant.getTenantName());
        response.setDescription(tenant.getDescription());
        response.setStatus(tenant.getStatus().name());
        response.setContactEmail(tenant.getContactEmail());
        response.setContactPhone(tenant.getContactPhone());
        response.setMaxUsers(tenant.getMaxUsers());
        response.setMaxDataSources(tenant.getMaxDataSources());
        response.setMaxApiServices(tenant.getMaxApiServices());
        response.setCreatedAt(tenant.getCreatedAt());
        return response;
    }
}