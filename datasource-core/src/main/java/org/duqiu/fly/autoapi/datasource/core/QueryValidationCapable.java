package org.duqiu.fly.autoapi.datasource.core;

/**
 * 查询验证能力接口 - 用于支持查询语句验证的数据源
 */
public interface QueryValidationCapable extends DataSourceConnection {
    
    /**
     * 验证查询语句语法是否正确
     */
    boolean validateQuery(String query);
    
    /**
     * 验证查询语句并返回详细信息
     */
    QueryValidationResult validateQueryDetailed(String query);
    
    /**
     * 查询验证结果
     */
    interface QueryValidationResult {
        boolean isValid();
        String getErrorMessage();
        int getErrorLine();
        int getErrorColumn();
    }
}