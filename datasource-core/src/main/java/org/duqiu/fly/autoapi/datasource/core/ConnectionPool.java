package org.duqiu.fly.autoapi.datasource.core;

/**
 * 连接池接口
 */
public interface ConnectionPool extends AutoCloseable {
    
    /**
     * 获取连接
     */
    DataSourceConnection getConnection();
    
    /**
     * 归还连接
     */
    void returnConnection(DataSourceConnection connection);
    
    /**
     * 获取连接池状态
     */
    PoolStatus getStatus();
    
    /**
     * 关闭连接池
     */
    @Override
    void close();
    
    /**
     * 连接池状态
     */
    interface PoolStatus {
        int getActiveConnections();
        int getIdleConnections();
        int getMaxConnections();
        int getMinConnections();
        boolean isHealthy();
    }
}