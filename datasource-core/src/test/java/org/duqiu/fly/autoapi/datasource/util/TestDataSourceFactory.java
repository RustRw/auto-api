package org.duqiu.fly.autoapi.datasource.util;

import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.model.DataSource;

/**
 * 测试数据源工厂类 - 用于创建各种测试场景的数据源
 */
public class TestDataSourceFactory {
    
    /**
     * 创建MySQL测试数据源请求
     */
    public static DataSourceCreateRequestV2 createMySqlRequest() {
        DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
        request.setName("Test MySQL DataSource");
        request.setDescription("MySQL datasource for integration testing");
        request.setType(DataSourceType.MYSQL);
        request.setHost("localhost");
        request.setPort(3306);
        request.setDatabase("testdb");
        request.setUsername("testuser");
        request.setPassword("testpass");
        request.setMaxPoolSize(10);
        request.setMinPoolSize(1);
        request.setConnectionTimeout(30000);
        request.setIdleTimeout(600000);
        request.setMaxLifetime(1800000);
        request.setSslEnabled(false);
        request.setConnectionPoolEnabled(true);
        return request;
    }
    
    /**
     * 创建PostgreSQL测试数据源请求
     */
    public static DataSourceCreateRequestV2 createPostgreSqlRequest() {
        DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
        request.setName("Test PostgreSQL DataSource");
        request.setDescription("PostgreSQL datasource for integration testing");
        request.setType(DataSourceType.POSTGRESQL);
        request.setHost("localhost");
        request.setPort(5432);
        request.setDatabase("testdb");
        request.setUsername("postgres");
        request.setPassword("testpass");
        request.setMaxPoolSize(15);
        request.setMinPoolSize(2);
        request.setConnectionTimeout(25000);
        request.setIdleTimeout(300000);
        request.setMaxLifetime(1200000);
        request.setSslEnabled(true);
        request.setConnectionPoolEnabled(true);
        return request;
    }
    
    /**
     * 创建MongoDB测试数据源请求
     */
    public static DataSourceCreateRequestV2 createMongoDbRequest() {
        DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
        request.setName("Test MongoDB DataSource");
        request.setDescription("MongoDB datasource for integration testing");
        request.setType(DataSourceType.MONGODB);
        request.setHost("localhost");
        request.setPort(27017);
        request.setDatabase("testdb");
        request.setUsername("mongouser");
        request.setPassword("mongopass");
        request.setSslEnabled(false);
        request.setConnectionPoolEnabled(true);
        return request;
    }
    
    /**
     * 创建Elasticsearch测试数据源请求
     */
    public static DataSourceCreateRequestV2 createElasticsearchRequest() {
        DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
        request.setName("Test Elasticsearch DataSource");
        request.setDescription("Elasticsearch datasource for integration testing");
        request.setType(DataSourceType.ELASTICSEARCH);
        request.setHost("localhost");
        request.setPort(9200);
        request.setUsername("elastic");
        request.setPassword("elasticpass");
        request.setSslEnabled(false);
        request.setConnectionPoolEnabled(false);
        return request;
    }
    
    /**
     * 创建无效配置的数据源请求（用于测试验证）
     */
    public static DataSourceCreateRequestV2 createInvalidRequest() {
        DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
        request.setName(""); // 无效的空名称
        request.setType(DataSourceType.MYSQL);
        request.setHost(""); // 无效的空主机
        request.setPort(0); // 无效端口
        request.setMaxPoolSize(0); // 无效的连接池大小
        request.setMinPoolSize(20); // 最小值大于最大值
        return request;
    }
    
    /**
     * 创建连接超时配置的数据源请求
     */
    public static DataSourceCreateRequestV2 createTimeoutRequest() {
        DataSourceCreateRequestV2 request = createMySqlRequest();
        request.setName("Timeout Test DataSource");
        request.setConnectionTimeout(1000); // 很短的超时时间
        request.setHost("192.168.255.254"); // 不可达的IP
        return request;
    }
    
    /**
     * 创建数据源实体
     */
    public static DataSource createDataSourceEntity(Long userId) {
        DataSource dataSource = new DataSource();
        dataSource.setName("Test DataSource Entity");
        dataSource.setDescription("Test datasource entity for integration testing");
        dataSource.setType(DataSourceType.MYSQL);
        dataSource.setHost("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabase("testdb");
        dataSource.setUsername("testuser");
        dataSource.setPassword("testpass");
        dataSource.setConnectionUrl("jdbc:h2:mem:testdb;MODE=MySQL");
        dataSource.setMaxPoolSize(10);
        dataSource.setMinPoolSize(1);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setSslEnabled(false);
        dataSource.setConnectionPoolEnabled(true);
        dataSource.setEnabled(true);
        dataSource.setCreatedBy(userId);
        dataSource.setUpdatedBy(userId);
        return dataSource;
    }
    
    /**
     * 创建H2模拟MySQL的数据源实体
     */
    public static DataSource createH2MockMySqlDataSource(Long userId, String h2Url) {
        DataSource dataSource = new DataSource();
        dataSource.setName("H2 Mock MySQL DataSource");
        dataSource.setDescription("H2 database mocking MySQL for testing");
        dataSource.setType(DataSourceType.MYSQL);
        dataSource.setHost("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabase("testdb");
        dataSource.setUsername("sa");
        dataSource.setPassword("password");
        dataSource.setConnectionUrl(h2Url);
        dataSource.setMaxPoolSize(5);
        dataSource.setMinPoolSize(1);
        dataSource.setConnectionTimeout(10000);
        dataSource.setIdleTimeout(300000);
        dataSource.setMaxLifetime(900000);
        dataSource.setSslEnabled(false);
        dataSource.setConnectionPoolEnabled(true);
        dataSource.setEnabled(true);
        dataSource.setCreatedBy(userId);
        dataSource.setUpdatedBy(userId);
        return dataSource;
    }
    
    /**
     * 创建批量测试数据源请求
     */
    public static DataSourceCreateRequestV2[] createBatchRequests(int count) {
        DataSourceCreateRequestV2[] requests = new DataSourceCreateRequestV2[count];
        
        for (int i = 0; i < count; i++) {
            DataSourceCreateRequestV2 request = createMySqlRequest();
            request.setName("Batch Test DataSource " + (i + 1));
            request.setDescription("Batch test datasource #" + (i + 1));
            request.setDatabase("testdb" + (i + 1));
            requests[i] = request;
        }
        
        return requests;
    }
    
    /**
     * 创建不同类型的数据源请求数组
     */
    public static DataSourceCreateRequestV2[] createMixedTypeRequests() {
        return new DataSourceCreateRequestV2[]{
                createMySqlRequest(),
                createPostgreSqlRequest(),
                createMongoDbRequest(),
                createElasticsearchRequest()
        };
    }
}