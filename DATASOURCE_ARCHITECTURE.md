# 数据源模块详细架构说明

## 架构概述

数据源模块采用统一的抽象架构，支持多种类型的数据源，包括关系型数据库、NoSQL数据库、时序数据库、图数据库、消息队列和HTTP接口等。

### 核心设计原则

1. **统一抽象**: 通过`DataSourceConnection`接口统一不同数据源的操作
2. **工厂模式**: 使用`UnifiedDataSourceFactory`根据数据源类型创建相应连接
3. **连接池管理**: 对JDBC类数据源提供HikariCP连接池支持
4. **版本管理**: 支持不同数据源的多版本驱动选择
5. **依赖检查**: 运行时检查必要依赖是否可用

## 支持的数据源类型

### 关系型数据库 (JDBC)
- **MySQL** - 主流开源关系型数据库
- **PostgreSQL** - 高级开源关系型数据库
- **Oracle** - 商业企业级关系型数据库

### 分析型数据库 (JDBC)
- **ClickHouse** - 高性能列式OLAP数据库
- **StarRocks** - 新一代极速全场景MPP数据库

### 时序数据库 (JDBC)
- **TDengine** - 高效的物联网大数据平台

### NoSQL数据库
- **MongoDB** - 文档型NoSQL数据库
- **Elasticsearch** - 分布式搜索和分析引擎

### 图数据库
- **NebulaGraph** - 开源分布式图数据库

### 消息队列
- **Kafka** - 高吞吐量分布式消息队列

### HTTP接口
- **HTTP API** - 标准HTTP接口
- **HTTPS API** - 加密HTTP接口

## 架构组件

### 1. 数据源枚举 (`DataSourceType`)

每个数据源类型包含以下信息：
- 显示名称和分类
- 协议类型（JDBC/HTTP/NATIVE）
- 驱动类名
- URL模板
- 支持的版本列表
- 默认端口
- 依赖坐标

```java
MYSQL("MySQL", DataSourceCategory.RELATIONAL_DB, DataSourceProtocol.JDBC, 
      "com.mysql.cj.jdbc.Driver", "jdbc:mysql://{host}:{port}/{database}",
      Arrays.asList("8.0", "5.7", "5.6"), 3306, "mysql:mysql-connector-java")
```

### 2. 统一连接接口 (`DataSourceConnection`)

提供统一的数据源操作接口：
```java
// 查询操作
QueryResult executeQuery(String query, Map<String, Object> parameters);

// 更新操作  
UpdateResult executeUpdate(String command, Map<String, Object> parameters);

// 连接测试
boolean isValid();

// 元数据获取
List<TableInfo> getTables();
TableSchema getTableSchema(String tableName);
```

### 3. 工厂模式 (`UnifiedDataSourceFactory`)

根据数据源类型创建相应的连接实现：
- **JDBC类**: 使用JDBC标准接口
- **HTTP类**: 使用RestTemplate实现
- **NATIVE类**: 使用各数据源的原生客户端

### 4. 连接池管理

#### JDBC连接池 (`HikariConnectionPool`)
- 基于HikariCP实现
- 支持连接池大小、超时时间等配置
- 针对不同数据库类型优化参数

```java
// MySQL优化配置
config.addDataSourceProperty("useServerPrepStmts", "true");
config.addDataSourceProperty("rewriteBatchedStatements", "true");
config.addDataSourceProperty("cacheResultSetMetadata", "true");
```

#### 连接池状态监控
- 活跃连接数
- 空闲连接数
- 连接池健康状态
- 性能指标

### 5. 版本和依赖管理

#### 依赖检查
```java
// 检查驱动类是否可用
Class.forName(type.getDriverClassName());

// 检查特定依赖
boolean isAvailable = dataSourceFactory.isDependencyAvailable(type);
```

#### 版本选择
每个数据源类型支持多个版本：
- 自动推荐最新稳定版本
- 支持用户选择特定版本
- 提供版本兼容性信息

## 使用方式

### 1. 创建数据源

```java
DataSourceCreateRequestV2 request = new DataSourceCreateRequestV2();
request.setName("MySQL测试库");
request.setType(DataSourceType.MYSQL);
request.setHost("localhost");
request.setPort(3306);
request.setDatabase("testdb");
request.setUsername("root");
request.setPassword("password");
request.setVersion("8.0");
```

### 2. 执行查询

```java
// 获取数据源连接
DataSourceConnection connection = factory.createConnection(dataSource);

// 执行查询
Map<String, Object> params = Map.of("id", 1);
QueryResult result = connection.executeQuery("SELECT * FROM users WHERE id = ?", params);

// 处理结果
List<Map<String, Object>> data = result.getData();
```

### 3. 获取元数据

```java
// 获取表列表
List<TableInfo> tables = connection.getTables();

// 获取表结构
TableSchema schema = connection.getTableSchema("users");
List<ColumnInfo> columns = schema.getColumns();
```

## 配置说明

### 连接池配置
```java
// 基本连接池配置
maxPoolSize: 10        // 最大连接数
minPoolSize: 1         // 最小连接数
connectionTimeout: 30000   // 连接超时(ms)
idleTimeout: 600000        // 空闲超时(ms)
maxLifetime: 1800000       // 连接最大生命周期(ms)
```

### 安全配置
```java
sslEnabled: false          // 是否启用SSL
username: "user"           // 用户名
password: "password"       // 密码
```

### 扩展配置
```java
additionalProperties: {    // 额外属性
  "useUnicode": "true",
  "characterEncoding": "utf8"
}
```

## API接口

### V2版本接口 (`/api/v2/datasources`)

#### 数据源管理
- `POST /` - 创建数据源
- `GET /` - 获取数据源列表
- `GET /{id}` - 获取数据源详情
- `DELETE /{id}` - 删除数据源

#### 连接测试和查询
- `POST /{id}/test-detailed` - 详细连接测试
- `GET /{id}/tables` - 获取表列表
- `GET /{id}/tables/{tableName}/schema` - 获取表结构
- `POST /{id}/query` - 执行查询

#### 类型和依赖管理
- `GET /types` - 获取支持的数据源类型
- `GET /types/{type}/dependency` - 获取依赖信息
- `GET /dependencies/check-all` - 检查所有依赖状态

## 依赖管理

### 必需依赖
```gradle
// 核心依赖
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'com.zaxxer:HikariCP'
implementation 'com.fasterxml.jackson.core:jackson-databind'
```

### 数据库驱动依赖
```gradle
// JDBC驱动
runtimeOnly 'mysql:mysql-connector-java:8.0.33'
runtimeOnly 'org.postgresql:postgresql:42.7.1'
runtimeOnly 'com.oracle.database.jdbc:ojdbc8:23.3.0.23.09'
runtimeOnly 'com.clickhouse:clickhouse-jdbc:0.6.0'
runtimeOnly 'com.taosdata.jdbc:taos-jdbcdriver:3.2.4'

// NoSQL客户端
implementation 'org.mongodb:mongodb-driver-sync:4.11.1'
implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
```

### 可选依赖
```gradle
// 图数据库 (按需启用)
// runtimeOnly 'com.vesoft:nebula-java:3.6.0'

// 消息队列 (按需启用)  
// implementation 'org.apache.kafka:kafka-clients:3.6.1'
```

## 扩展指南

### 添加新数据源类型

1. **在`DataSourceType`枚举中添加新类型**
```java
NEW_DB("NewDB", DataSourceCategory.RELATIONAL_DB, DataSourceProtocol.JDBC,
       "com.newdb.Driver", "jdbc:newdb://{host}:{port}/{database}",
       Arrays.asList("1.0", "2.0"), 5432, "com.newdb:newdb-driver")
```

2. **实现连接类**
```java
public class NewDbConnection implements DataSourceConnection {
    // 实现所有接口方法
}
```

3. **在工厂中添加创建逻辑**
```java
case NEW_DB:
    return createNewDbConnection(dataSource);
```

4. **添加必要依赖**
```gradle
runtimeOnly 'com.newdb:newdb-driver:2.0'
```

### 性能优化建议

1. **连接池优化**
   - 根据应用负载调整连接池大小
   - 设置合适的连接超时和生命周期
   - 监控连接池状态

2. **查询优化**  
   - 使用参数化查询
   - 合理使用事务
   - 避免长时间持有连接

3. **监控和日志**
   - 记录连接创建和销毁
   - 监控查询执行时间
   - 定期检查连接池健康状态

## 故障排除

### 常见问题

1. **驱动未找到**
   - 检查依赖是否正确添加
   - 确认驱动版本与数据库版本兼容

2. **连接失败**
   - 验证主机地址和端口
   - 检查用户名密码
   - 确认网络连通性

3. **连接池耗尽**
   - 增加最大连接数
   - 检查是否有连接泄漏
   - 调整连接超时时间

### 调试技巧

1. **启用详细日志**
```properties
logging.level.org.duqiu.fly.autoapi.datasource=DEBUG
logging.level.com.zaxxer.hikari=DEBUG
```

2. **连接测试**
```java
// 使用详细测试接口
ConnectionStatus status = dataSourceService.testConnectionDetailed(id, userId);
```

3. **依赖检查**
```java  
// 检查特定类型依赖
boolean available = factory.isDependencyAvailable(DataSourceType.MYSQL);
```

这个数据源模块提供了完整、灵活、可扩展的多数据源管理能力，支持企业级应用的各种数据源需求。