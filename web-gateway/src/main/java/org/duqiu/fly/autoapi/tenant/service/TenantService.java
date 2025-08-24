package org.duqiu.fly.autoapi.tenant.service;

import org.duqiu.fly.autoapi.auth.repository.TenantRepository;
import org.duqiu.fly.autoapi.auth.repository.UserRepository;
import org.duqiu.fly.autoapi.common.model.Tenant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 租户管理服务
 */
@Service
@Transactional
public class TenantService {
    
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    
    public TenantService(TenantRepository tenantRepository, UserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * 创建新租户
     */
    public Tenant createTenant(String tenantCode, String tenantName, String description, 
                             String contactEmail, Long createdBy) {
        if (tenantRepository.existsByTenantCode(tenantCode)) {
            throw new RuntimeException("租户代码已存在");
        }
        
        Tenant tenant = new Tenant();
        tenant.setTenantCode(tenantCode);
        tenant.setTenantName(tenantName);
        tenant.setDescription(description);
        tenant.setContactEmail(contactEmail);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setCreatedBy(createdBy);
        tenant.setUpdatedBy(createdBy);
        
        return tenantRepository.save(tenant);
    }
    
    /**
     * 根据租户代码获取租户
     */
    public Optional<Tenant> getTenantByCode(String tenantCode) {
        return tenantRepository.findByTenantCodeAndStatus(tenantCode, Tenant.TenantStatus.ACTIVE);
    }
    
    /**
     * 检查租户是否可用
     */
    public boolean isTenantActive(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .map(tenant -> tenant.getStatus() == Tenant.TenantStatus.ACTIVE)
                .orElse(false);
    }
    
    /**
     * 暂停租户
     */
    public void suspendTenant(Long tenantId, Long updatedBy) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("租户不存在"));
        
        tenant.setStatus(Tenant.TenantStatus.SUSPENDED);
        tenant.setUpdatedBy(updatedBy);
        tenantRepository.save(tenant);
    }
    
    /**
     * 激活租户
     */
    public void activateTenant(Long tenantId, Long updatedBy) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("租户不存在"));
        
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setUpdatedBy(updatedBy);
        tenantRepository.save(tenant);
    }
    
    /**
     * 检查租户的用户数量限制
     */
    public boolean canAddUser(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("租户不存在"));
        
        long currentUserCount = userRepository.countByTenantId(tenantId);
        return currentUserCount < tenant.getMaxUsers();
    }
    
    /**
     * 获取租户当前用户数量
     */
    public long getTenantUserCount(Long tenantId) {
        return userRepository.countByTenantId(tenantId);
    }
}