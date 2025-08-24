package org.duqiu.fly.autoapi.common.context;

/**
 * 租户上下文管理器
 */
public class TenantContext {
    
    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_TENANT_CODE = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();
    
    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }
    
    public static Long getTenantId() {
        return CURRENT_TENANT_ID.get();
    }
    
    public static void setTenantCode(String tenantCode) {
        CURRENT_TENANT_CODE.set(tenantCode);
    }
    
    public static String getTenantCode() {
        return CURRENT_TENANT_CODE.get();
    }
    
    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }
    
    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }
    
    public static void clear() {
        CURRENT_TENANT_ID.remove();
        CURRENT_TENANT_CODE.remove();
        CURRENT_USER_ID.remove();
    }
    
    public static boolean hasTenant() {
        return CURRENT_TENANT_ID.get() != null;
    }
}