package org.duqiu.fly.autoapi.api.dto;

import lombok.Data;

import java.util.List;

/**
 * API服务版本对比响应DTO
 */
@Data
public class ApiServiceVersionCompareResponse {
    
    /**
     * 源版本信息
     */
    private ApiServiceVersionResponse sourceVersion;
    
    /**
     * 目标版本信息
     */
    private ApiServiceVersionResponse targetVersion;
    
    /**
     * 差异列表
     */
    private List<VersionDifference> differences;
    
    /**
     * 版本差异详情
     */
    @Data
    public static class VersionDifference {
        /**
         * 字段名
         */
        private String fieldName;
        
        /**
         * 字段显示名
         */
        private String fieldDisplayName;
        
        /**
         * 源版本值
         */
        private Object sourceValue;
        
        /**
         * 目标版本值
         */
        private Object targetValue;
        
        /**
         * 差异类型
         */
        private DifferenceType differenceType;
    }
    
    /**
     * 差异类型枚举
     */
    public enum DifferenceType {
        ADDED("新增"),
        REMOVED("删除"),
        MODIFIED("修改"),
        UNCHANGED("未变化");
        
        private final String description;
        
        DifferenceType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}