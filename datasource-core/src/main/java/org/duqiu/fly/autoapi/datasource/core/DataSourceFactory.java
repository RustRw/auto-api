package org.duqiu.fly.autoapi.datasource.core;

import org.duqiu.fly.autoapi.datasource.model.DataSource;

/**
 * 数据源工厂接口
 */
public interface DataSourceFactory {
    
    /**
     * 创建数据源连接
     */
    DataSourceConnection createConnection(DataSource dataSource);
    
    /**
     * 测试数据源连接
     */
    boolean testConnection(DataSource dataSource);
    
    /**
     * 获取连接池
     */
    ConnectionPool getConnectionPool(DataSource dataSource);
    
    /**
     * 验证数据源配置
     */
    ValidationResult validateConfiguration(DataSource dataSource);
    
    /**
     * 生成连接URL
     */
    String buildConnectionUrl(DataSource dataSource);
    
    /**
     * 验证结果
     */
    interface ValidationResult {
        boolean isValid();
        String getErrorMessage();
        String getRecommendation();
    }
}