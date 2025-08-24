package org.duqiu.fly.autoapi.datasource.factory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.duqiu.fly.autoapi.datasource.core.ConnectionPool;
import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.core.DataSourceFactory;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.http.HttpConnection;
import org.duqiu.fly.autoapi.datasource.jdbc.JdbcConnection;
import org.duqiu.fly.autoapi.datasource.jdbc.JdbcDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.nosql.ElasticsearchConnection;
import org.duqiu.fly.autoapi.datasource.nosql.MongoConnection;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一数据源工厂 - 根据数据源类型创建相应的连接
 */
@Component
public class UnifiedDataSourceFactory implements DataSourceFactory {
    
    private final JdbcDataSourceFactory jdbcFactory;
    private final ConcurrentHashMap<String, Object> connectionCache = new ConcurrentHashMap<>();
    
    public UnifiedDataSourceFactory() {
        this.jdbcFactory = new JdbcDataSourceFactory();
    }
    
    @Override
    public DataSourceConnection createConnection(DataSource dataSource) {
        DataSourceType type = dataSource.getType();
        
        try {
            switch (type.getProtocol()) {
                case JDBC:
                    return createJdbcConnection(dataSource);
                case HTTP:
                    return createHttpConnection(dataSource);
                case NATIVE:
                    return createNativeConnection(dataSource);
                default:
                    throw new UnsupportedOperationException("不支持的数据源协议: " + type.getProtocol());
            }
        } catch (Exception e) {
            throw new RuntimeException("创建数据源连接失败: " + e.getMessage(), e);
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
        if (dataSource.getType().isJdbcType()) {
            return jdbcFactory.getConnectionPool(dataSource);
        }
        
        // 对于非JDBC类型，返回简单的连接池实现或null
        return null;
    }
    
    @Override
    public ValidationResult validateConfiguration(DataSource dataSource) {
        DataSourceType type = dataSource.getType();
        
        try {
            // 通用验证
            if (dataSource.getHost() == null || dataSource.getHost().isEmpty()) {
                return new UnifiedValidationResult(false, "主机地址不能为空", "请提供有效的主机地址");
            }
            
            if (dataSource.getPort() == null || dataSource.getPort() <= 0) {
                return new UnifiedValidationResult(false, "端口号无效", "请提供有效的端口号(1-65535)");
            }
            
            // 协议特定验证
            switch (type.getProtocol()) {
                case JDBC:
                    return validateJdbcConfiguration(dataSource);
                case HTTP:
                    return validateHttpConfiguration(dataSource);
                case NATIVE:
                    return validateNativeConfiguration(dataSource);
                default:
                    return new UnifiedValidationResult(false, "不支持的数据源类型", "请选择支持的数据源类型");
            }
        } catch (Exception e) {
            return new UnifiedValidationResult(false, "配置验证失败: " + e.getMessage(), 
                                              "请检查数据源配置");
        }
    }
    
    @Override
    public String buildConnectionUrl(DataSource dataSource) {
        String template = dataSource.getType().getUrlTemplate();
        String url = template.replace("{host}", dataSource.getHost())
                            .replace("{port}", dataSource.getPort().toString());
        
        if (template.contains("{database}")) {
            url = url.replace("{database}", dataSource.getDatabase() != null ? dataSource.getDatabase() : "");
        }
        
        return url;
    }
    
    private DataSourceConnection createJdbcConnection(DataSource dataSource) throws Exception {
        // 检查并加载JDBC驱动
        String driverClassName = dataSource.getType().getDriverClassName();
        if (driverClassName != null && !driverClassName.isEmpty()) {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("JDBC驱动未找到: " + driverClassName + 
                                         ", 请添加依赖: " + dataSource.getType().getDependencyCoordinate());
            }
        }
        
        String url = buildConnectionUrl(dataSource);
        Connection connection = DriverManager.getConnection(
            url, dataSource.getUsername(), dataSource.getPassword());
        
        return new JdbcConnection(connection);
    }
    
    private DataSourceConnection createHttpConnection(DataSource dataSource) {
        String baseUrl = buildConnectionUrl(dataSource);
        return new HttpConnection(baseUrl, dataSource.getUsername(), dataSource.getPassword());
    }
    
    private DataSourceConnection createNativeConnection(DataSource dataSource) throws Exception {
        DataSourceType type = dataSource.getType();
        
        switch (type) {
            case MONGODB:
                return createMongoConnection(dataSource);
            case NEBULA_GRAPH:
                return createNebulaConnection(dataSource);
            case KAFKA:
                return createKafkaConnection(dataSource);
            default:
                throw new UnsupportedOperationException("不支持的原生数据源类型: " + type);
        }
    }
    
    private DataSourceConnection createMongoConnection(DataSource dataSource) {
        try {
            String connectionString = buildMongoConnectionString(dataSource);
            MongoClient mongoClient = MongoClients.create(connectionString);
            return new MongoConnection(mongoClient, dataSource.getDatabase());
        } catch (Exception e) {
            throw new RuntimeException("创建MongoDB连接失败: " + e.getMessage() + 
                                     ", 请添加依赖: " + dataSource.getType().getDependencyCoordinate(), e);
        }
    }
    
    private DataSourceConnection createNebulaConnection(DataSource dataSource) {
        // NebulaGraph连接实现
        throw new UnsupportedOperationException("NebulaGraph连接暂未实现");
    }
    
    private DataSourceConnection createKafkaConnection(DataSource dataSource) {
        // Kafka连接实现
        throw new UnsupportedOperationException("Kafka连接暂未实现");
    }
    
    private String buildMongoConnectionString(DataSource dataSource) {
        StringBuilder sb = new StringBuilder("mongodb://");
        
        if (dataSource.getUsername() != null && !dataSource.getUsername().isEmpty()) {
            sb.append(dataSource.getUsername());
            if (dataSource.getPassword() != null && !dataSource.getPassword().isEmpty()) {
                sb.append(":").append(dataSource.getPassword());
            }
            sb.append("@");
        }
        
        sb.append(dataSource.getHost()).append(":").append(dataSource.getPort());
        
        if (dataSource.getDatabase() != null && !dataSource.getDatabase().isEmpty()) {
            sb.append("/").append(dataSource.getDatabase());
        }
        
        return sb.toString();
    }
    
    private ValidationResult validateJdbcConfiguration(DataSource dataSource) {
        if (dataSource.getUsername() == null || dataSource.getUsername().isEmpty()) {
            return new UnifiedValidationResult(false, "用户名不能为空", "请提供有效的数据库用户名");
        }
        
        // 检查驱动类
        String driverClassName = dataSource.getType().getDriverClassName();
        if (driverClassName != null && !driverClassName.isEmpty()) {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                return new UnifiedValidationResult(false, "JDBC驱动未找到: " + driverClassName, 
                                                  "请添加依赖: " + dataSource.getType().getDependencyCoordinate());
            }
        }
        
        return new UnifiedValidationResult(true, "JDBC配置验证通过", null);
    }
    
    private ValidationResult validateHttpConfiguration(DataSource dataSource) {
        String url = buildConnectionUrl(dataSource);
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return new UnifiedValidationResult(false, "HTTP URL格式错误", "URL必须以http://或https://开头");
        }
        
        return new UnifiedValidationResult(true, "HTTP配置验证通过", null);
    }
    
    private ValidationResult validateNativeConfiguration(DataSource dataSource) {
        DataSourceType type = dataSource.getType();
        
        switch (type) {
            case MONGODB:
                if (dataSource.getDatabase() == null || dataSource.getDatabase().isEmpty()) {
                    return new UnifiedValidationResult(false, "MongoDB数据库名不能为空", 
                                                      "请指定要连接的MongoDB数据库名");
                }
                break;
            case ELASTICSEARCH:
                // Elasticsearch不需要特殊验证
                break;
            default:
                return new UnifiedValidationResult(false, "不支持的原生数据源类型: " + type, 
                                                  "请选择支持的数据源类型");
        }
        
        return new UnifiedValidationResult(true, "原生数据源配置验证通过", null);
    }
    
    /**
     * 获取数据源类型的版本依赖信息
     */
    public DependencyInfo getDependencyInfo(DataSourceType type) {
        return new DependencyInfo(
            type.getDependencyCoordinate(),
            type.getSupportedVersions(),
            type.getSupportedVersions().get(0) // 默认推荐最新版本
        );
    }
    
    /**
     * 检查数据源类型的依赖是否可用
     */
    public boolean isDependencyAvailable(DataSourceType type) {
        try {
            switch (type.getProtocol()) {
                case JDBC:
                    if (type.getDriverClassName() != null && !type.getDriverClassName().isEmpty()) {
                        Class.forName(type.getDriverClassName());
                    }
                    return true;
                case HTTP:
                    // HTTP依赖Spring Web，通常都可用
                    return true;
                case NATIVE:
                    return checkNativeDependency(type);
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean checkNativeDependency(DataSourceType type) {
        try {
            switch (type) {
                case MONGODB:
                    Class.forName("com.mongodb.client.MongoClient");
                    return true;
                case ELASTICSEARCH:
                    Class.forName("org.elasticsearch.client.RestHighLevelClient");
                    return true;
                case NEBULA_GRAPH:
                    Class.forName("com.vesoft.nebula.client.graph.NebulaPoolConfig");
                    return true;
                case KAFKA:
                    Class.forName("org.apache.kafka.clients.producer.KafkaProducer");
                    return true;
                default:
                    return false;
            }
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * 依赖信息
     */
    public static class DependencyInfo {
        private final String coordinate;
        private final java.util.List<String> supportedVersions;
        private final String recommendedVersion;
        
        public DependencyInfo(String coordinate, java.util.List<String> supportedVersions, String recommendedVersion) {
            this.coordinate = coordinate;
            this.supportedVersions = supportedVersions;
            this.recommendedVersion = recommendedVersion;
        }
        
        public String getCoordinate() { return coordinate; }
        public java.util.List<String> getSupportedVersions() { return supportedVersions; }
        public String getRecommendedVersion() { return recommendedVersion; }
    }
    
    static class UnifiedValidationResult implements ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String recommendation;
        
        public UnifiedValidationResult(boolean valid, String errorMessage, String recommendation) {
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