package org.duqiu.fly.autoapi.datasource.jdbc;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.duqiu.fly.autoapi.datasource.core.ConnectionPool;
import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP连接池实现
 */
public class HikariConnectionPool implements ConnectionPool {
    
    private final HikariDataSource dataSource;
    private final HikariPoolMXBean poolBean;
    
    public HikariConnectionPool(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.poolBean = dataSource.getHikariPoolMXBean();
    }
    
    @Override
    public DataSourceConnection getConnection() {
        try {
            Connection connection = dataSource.getConnection();
            return new JdbcConnection(connection);
        } catch (SQLException e) {
            throw new RuntimeException("获取数据库连接失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void returnConnection(DataSourceConnection connection) {
        // HikariCP会自动管理连接的归还，这里主要是关闭连接
        connection.close();
    }
    
    @Override
    public PoolStatus getStatus() {
        return new HikariPoolStatus(
            poolBean.getActiveConnections(),
            poolBean.getIdleConnections(),
            poolBean.getTotalConnections(),
            dataSource.getMinimumIdle(),
            !dataSource.isClosed() && dataSource.isRunning()
        );
    }
    
    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }
    
    public HikariDataSource getDataSource() {
        return dataSource;
    }
    
    static class HikariPoolStatus implements PoolStatus {
        private final int activeConnections;
        private final int idleConnections;
        private final int maxConnections;
        private final int minConnections;
        private final boolean healthy;
        
        public HikariPoolStatus(int activeConnections, int idleConnections, 
                               int maxConnections, int minConnections, boolean healthy) {
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.maxConnections = maxConnections;
            this.minConnections = minConnections;
            this.healthy = healthy;
        }
        
        @Override
        public int getActiveConnections() { return activeConnections; }
        @Override
        public int getIdleConnections() { return idleConnections; }
        @Override
        public int getMaxConnections() { return maxConnections; }
        @Override
        public int getMinConnections() { return minConnections; }
        @Override
        public boolean isHealthy() { return healthy; }
    }
}