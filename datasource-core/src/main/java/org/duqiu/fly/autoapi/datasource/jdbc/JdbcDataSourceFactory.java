package org.duqiu.fly.autoapi.datasource.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.duqiu.fly.autoapi.datasource.core.ConnectionPool;
import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.core.DataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDBC数据源工厂实现
 */
@Component
public class JdbcDataSourceFactory implements DataSourceFactory {
    
    private final ConcurrentHashMap<String, HikariConnectionPool> connectionPools = new ConcurrentHashMap<>();
    
    @Override
    public DataSourceConnection createConnection(DataSource dataSource) {
        try {
            // 加载驱动
            Class.forName(dataSource.getType().getDriverClassName());
            
            String url = buildConnectionUrl(dataSource);
            Connection connection = DriverManager.getConnection(
                url, dataSource.getUsername(), dataSource.getPassword());
            
            return new JdbcConnection(connection);
        } catch (Exception e) {
            throw new RuntimeException("创建JDBC连接失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean testConnection(DataSource dataSource) {
        try (DataSourceConnection connection = createConnection(dataSource)) {
            return connection.isValid();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public ConnectionPool getConnectionPool(DataSource dataSource) {
        String poolKey = generatePoolKey(dataSource);
        return connectionPools.computeIfAbsent(poolKey, k -> createConnectionPool(dataSource));
    }
    
    @Override
    public ValidationResult validateConfiguration(DataSource dataSource) {
        try {
            // 验证驱动类是否存在
            Class.forName(dataSource.getType().getDriverClassName());
            
            // 验证URL格式
            String url = buildConnectionUrl(dataSource);
            if (url.isEmpty()) {
                return new JdbcValidationResult(false, "连接URL不能为空", "请检查主机、端口和数据库名称");
            }
            
            // 验证连接参数
            if (dataSource.getUsername() == null || dataSource.getUsername().isEmpty()) {
                return new JdbcValidationResult(false, "用户名不能为空", "请提供有效的数据库用户名");
            }
            
            return new JdbcValidationResult(true, "配置验证通过", null);
        } catch (ClassNotFoundException e) {
            return new JdbcValidationResult(false, "JDBC驱动未找到: " + e.getMessage(), 
                                           "请确保相关驱动已添加到classpath中");
        } catch (Exception e) {
            return new JdbcValidationResult(false, "配置验证失败: " + e.getMessage(), 
                                           "请检查数据源配置");
        }
    }
    
    @Override
    public String buildConnectionUrl(DataSource dataSource) {
        String template = dataSource.getType().getUrlTemplate();
        return template.replace("{host}", dataSource.getHost())
                      .replace("{port}", dataSource.getPort().toString())
                      .replace("{database}", dataSource.getDatabase() != null ? dataSource.getDatabase() : "");
    }
    
    private HikariConnectionPool createConnectionPool(DataSource dataSource) {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(buildConnectionUrl(dataSource));
        config.setUsername(dataSource.getUsername());
        config.setPassword(dataSource.getPassword());
        config.setDriverClassName(dataSource.getType().getDriverClassName());
        
        // 连接池配置
        config.setMaximumPoolSize(dataSource.getMaxPoolSize() != null ? dataSource.getMaxPoolSize() : 10);
        config.setMinimumIdle(Math.max(1, config.getMaximumPoolSize() / 4));
        config.setConnectionTimeout(dataSource.getConnectionTimeout() != null ? dataSource.getConnectionTimeout() : 30000);
        config.setIdleTimeout(600000); // 10分钟
        config.setMaxLifetime(1800000); // 30分钟
        config.setLeakDetectionThreshold(60000); // 1分钟
        
        // 连接验证
        config.setConnectionTestQuery(dataSource.getTestQuery());
        
        // 性能配置
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        // 根据数据库类型进行特殊配置
        configureByDatabaseType(config, dataSource);
        
        HikariDataSource hikariDataSource = new HikariDataSource(config);
        return new HikariConnectionPool(hikariDataSource);
    }
    
    private void configureByDatabaseType(HikariConfig config, DataSource dataSource) {
        switch (dataSource.getType()) {
            case MYSQL:
            case STARROCKS:
                config.addDataSourceProperty("useServerPrepStmts", "true");
                config.addDataSourceProperty("useLocalSessionState", "true");
                config.addDataSourceProperty("rewriteBatchedStatements", "true");
                config.addDataSourceProperty("cacheResultSetMetadata", "true");
                config.addDataSourceProperty("cacheServerConfiguration", "true");
                config.addDataSourceProperty("maintainTimeStats", "false");
                break;
                
            case POSTGRESQL:
                config.addDataSourceProperty("reWriteBatchedInserts", "true");
                config.addDataSourceProperty("stringtype", "unspecified");
                break;
                
            case CLICKHOUSE:
                config.addDataSourceProperty("socket_timeout", "600000");
                config.addDataSourceProperty("connection_timeout", "60000");
                break;
                
            case ORACLE:
                config.addDataSourceProperty("oracle.jdbc.implicitStatementCacheSize", "25");
                config.addDataSourceProperty("oracle.jdbc.defaultRowPrefetch", "25");
                break;
                
            case TDENGINE:
                config.addDataSourceProperty("charset", "UTF-8");
                config.addDataSourceProperty("locale", "en_US.UTF-8");
                config.addDataSourceProperty("timezone", "UTC-8");
                break;
        }
    }
    
    private String generatePoolKey(DataSource dataSource) {
        return String.format("%s_%s_%s_%s", 
            dataSource.getType().name(),
            dataSource.getHost(),
            dataSource.getPort(),
            dataSource.getDatabase());
    }
    
    public void closeAllPools() {
        connectionPools.values().forEach(HikariConnectionPool::close);
        connectionPools.clear();
    }
    
    static class JdbcValidationResult implements ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String recommendation;
        
        public JdbcValidationResult(boolean valid, String errorMessage, String recommendation) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.recommendation = recommendation;
        }
        
        @Override
        public boolean isValid() { return valid; }
        @Override
        public String getErrorMessage() { return errorMessage; }
        @Override
        public String getRecommendation() { return recommendation; }
    }
}