package org.duqiu.fly.autoapi.datasource.enums;

import java.util.Arrays;
import java.util.List;

public enum DataSourceType {
    // 关系型数据库 - JDBC
    MYSQL("MySQL", DataSourceCategory.RELATIONAL_DB, DataSourceProtocol.JDBC, 
          "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}",
          Arrays.asList("8.0", "5.7", "5.6"), 3306, "mysql:mysql-connector-java"),
          
    POSTGRESQL("PostgreSQL", DataSourceCategory.RELATIONAL_DB, DataSourceProtocol.JDBC,
               "org.postgresql.Driver", "jdbc:postgresql://{host}:{port}/{database}",
               Arrays.asList("15", "14", "13", "12"), 5432, "org.postgresql:postgresql"),
               
    ORACLE("Oracle", DataSourceCategory.RELATIONAL_DB, DataSourceProtocol.JDBC,
           "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@{host}:{port}:{database}",
           Arrays.asList("21c", "19c", "18c", "12c"), 1521, "com.oracle.database.jdbc:ojdbc8"),
           
    H2("H2", DataSourceCategory.RELATIONAL_DB, DataSourceProtocol.JDBC,
       "org.h2.Driver", "jdbc:h2:mem:{database}",
       Arrays.asList("2.2", "2.1", "1.4"), 8082, "com.h2database:h2"),

    // 分析型数据库
    CLICKHOUSE("ClickHouse", DataSourceCategory.ANALYTICAL_DB, DataSourceProtocol.JDBC,
               "com.clickhouse.jdbc.ClickHouseDriver", "jdbc:clickhouse://{host}:{port}/{database}",
               Arrays.asList("23.8", "23.3", "22.8"), 8123, "com.clickhouse:clickhouse-jdbc"),
               
    STARROCKS("StarRocks", DataSourceCategory.ANALYTICAL_DB, DataSourceProtocol.JDBC,
              "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}",
              Arrays.asList("3.1", "3.0", "2.5"), 9030, "mysql:mysql-connector-java"),

    // 时序数据库
    TDENGINE("TDengine", DataSourceCategory.TIME_SERIES_DB, DataSourceProtocol.JDBC,
             "com.taosdata.jdbc.TSDBDriver", "jdbc:TAOS-RS://{host}:{port}/{database}",
             Arrays.asList("3.0", "2.6", "2.4"), 6041, "com.taosdata.jdbc:taos-jdbcdriver"),

    // NoSQL数据库
    MONGODB("MongoDB", DataSourceCategory.NOSQL_DB, DataSourceProtocol.NATIVE,
            "", "mongodb://{host}:{port}/{database}",
            Arrays.asList("7.0", "6.0", "5.0", "4.4"), 27017, "org.mongodb:mongodb-driver-sync"),
            
    ELASTICSEARCH("Elasticsearch", DataSourceCategory.NOSQL_DB, DataSourceProtocol.HTTP,
                  "", "http://{host}:{port}",
                  Arrays.asList("8.11", "8.8", "7.17", "7.10"), 9200, "org.elasticsearch.client:elasticsearch-rest-high-level-client"),

    // 图数据库
    NEBULA_GRAPH("NebulaGraph", DataSourceCategory.GRAPH_DB, DataSourceProtocol.NATIVE,
                 "", "{host}:{port}",
                 Arrays.asList("3.6", "3.5", "3.4"), 9669, "com.vesoft:nebula-java"),

    // 消息队列
    KAFKA("Kafka", DataSourceCategory.MESSAGE_QUEUE, DataSourceProtocol.NATIVE,
          "", "{host}:{port}",
          Arrays.asList("3.6", "3.5", "3.4", "2.8"), 9092, "org.apache.kafka:kafka-clients"),

    // HTTP接口
    HTTP_API("HTTP API", DataSourceCategory.HTTP_API, DataSourceProtocol.HTTP,
             "", "http://{host}:{port}",
             Arrays.asList("1.1", "2.0"), 80, "org.springframework:spring-web"),
             
    HTTPS_API("HTTPS API", DataSourceCategory.HTTP_API, DataSourceProtocol.HTTP,
              "", "https://{host}:{port}",
              Arrays.asList("1.1", "2.0"), 443, "org.springframework:spring-web");

    private final String displayName;
    private final DataSourceCategory category;
    private final DataSourceProtocol protocol;
    private final String driverClassName;
    private final String urlTemplate;
    private final List<String> supportedVersions;
    private final int defaultPort;
    private final String dependencyCoordinate;

    DataSourceType(String displayName, DataSourceCategory category, DataSourceProtocol protocol,
                   String driverClassName, String urlTemplate, List<String> supportedVersions,
                   int defaultPort, String dependencyCoordinate) {
        this.displayName = displayName;
        this.category = category;
        this.protocol = protocol;
        this.driverClassName = driverClassName;
        this.urlTemplate = urlTemplate;
        this.supportedVersions = supportedVersions;
        this.defaultPort = defaultPort;
        this.dependencyCoordinate = dependencyCoordinate;
    }

    // Getters
    public String getDisplayName() { return displayName; }
    public DataSourceCategory getCategory() { return category; }
    public DataSourceProtocol getProtocol() { return protocol; }
    public String getDriverClassName() { return driverClassName; }
    public String getUrlTemplate() { return urlTemplate; }
    public List<String> getSupportedVersions() { return supportedVersions; }
    public int getDefaultPort() { return defaultPort; }
    public String getDependencyCoordinate() { return dependencyCoordinate; }

    public boolean isJdbcType() {
        return protocol == DataSourceProtocol.JDBC;
    }

    public boolean isHttpType() {
        return protocol == DataSourceProtocol.HTTP;
    }

    public boolean isNativeType() {
        return protocol == DataSourceProtocol.NATIVE;
    }
}