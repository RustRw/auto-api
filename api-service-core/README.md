# API Service Core 模块

API Service Core 模块是 Auto API 系统的核心组件，提供基于数据源的API服务管理功能。

## 功能特性

### 1. API服务管理
- **CRUD操作**: 创建、读取、更新、删除API服务
- **状态管理**: 草稿、测试中、已发布、已禁用四种状态
- **权限控制**: 基于创建者的权限验证

### 2. 版本管理
- **版本发布**: 将草稿状态的API服务发布为特定版本
- **版本快照**: 记录每次发布时的完整配置信息
- **版本对比**: 支持任意两个版本之间的详细对比
- **版本激活**: 支持切换激活版本

### 3. 在线测试
- **草稿测试**: 测试未发布的API服务
- **版本测试**: 测试已发布的特定版本
- **SQL验证**: 验证SQL语法正确性
- **执行计划**: 获取SQL执行计划
- **批量测试**: 支持多组参数批量测试

### 4. 表选择功能
- **可视化选择**: 基于数据源选择库表和字段
- **关联配置**: 支持多表JOIN查询配置
- **SQL生成**: 自动生成SQL语句模板

### 5. 审计日志
- **操作记录**: 记录所有CRUD操作
- **测试日志**: 记录所有测试操作
- **性能统计**: 记录操作耗时
- **错误追踪**: 详细的错误信息记录

## 核心实体

### ApiService
API服务主实体，包含基本配置信息：
```java
public class ApiService {
    private String name;           // 服务名称
    private String description;    // 服务描述  
    private String path;           // API路径
    private HttpMethod method;     // HTTP方法
    private Long dataSourceId;     // 数据源ID
    private String sqlContent;     // SQL内容
    private ApiStatus status;      // 服务状态
    // ... 其他配置字段
}
```

### ApiServiceVersion
API服务版本实体，记录每次发布的快照：
```java
public class ApiServiceVersion {
    private String version;        // 版本号
    private Boolean isActive;      // 是否为激活版本
    private LocalDateTime publishedAt; // 发布时间
    // ... 快照字段与ApiService相同
}
```

### ApiServiceAuditLog
审计日志实体，记录所有操作：
```java
public class ApiServiceAuditLog {
    private OperationType operationType;  // 操作类型
    private String operationDescription;  // 操作描述
    private OperationResult operationResult; // 操作结果
    private Long durationMs;              // 执行时长
    // ... 其他审计字段
}
```

## API接口

### 1. API服务管理接口

#### 创建API服务
```http
POST /api/v1/services
Content-Type: application/json
X-User-Id: 1

{
  "name": "用户查询服务",
  "description": "根据ID查询用户信息",
  "path": "/api/users/{id}",
  "method": "GET",
  "dataSourceId": 1,
  "sqlContent": "SELECT * FROM users WHERE id = ${id}",
  "requestParams": "{\"id\": {\"type\": \"long\", \"required\": true}}",
  "cacheEnabled": true,
  "cacheDuration": 300
}
```

#### 更新API服务
```http
PUT /api/v1/services/1
Content-Type: application/json
X-User-Id: 1

{
  "name": "用户详情查询服务",
  "sqlContent": "SELECT u.*, p.name as profile_name FROM users u LEFT JOIN profiles p ON u.id = p.user_id WHERE u.id = ${id}",
  "updateDescription": "增加了用户档案信息关联查询"
}
```

#### 发布API服务
```http
POST /api/v1/services/1/publish
Content-Type: application/json
X-User-Id: 1

{
  "version": "1.0.0",
  "versionDescription": "初始版本发布",
  "forcePublish": false
}
```

#### 下线API服务
```http
POST /api/v1/services/1/unpublish
X-User-Id: 1
```

### 2. 版本管理接口

#### 获取版本列表
```http
GET /api/v1/services/1/versions?page=0&size=10
X-User-Id: 1
```

#### 版本对比
```http
GET /api/v1/services/1/versions/compare?sourceVersion=1.0.0&targetVersion=1.1.0
X-User-Id: 1
```

### 3. 测试接口

#### 测试草稿API
```http
POST /api/v1/testing/draft
Content-Type: application/json
X-User-Id: 1

{
  "apiServiceId": 1,
  "parameters": {
    "id": 123,
    "status": "active"
  }
}
```

#### 测试已发布API
```http
POST /api/v1/testing/published/1?version=1.0.0
Content-Type: application/json
X-User-Id: 1

{
  "id": 123,
  "status": "active"
}
```

#### SQL验证
```http
POST /api/v1/testing/validate-sql?dataSourceId=1
Content-Type: application/json
X-User-Id: 1

{
  "sql": "SELECT * FROM users WHERE id = ${id}",
  "parameters": {
    "id": 123
  }
}
```

### 4. 表选择接口

#### 获取数据源表列表
```http
GET /api/v1/table-selection/datasource/1/tables?database=mydb&schema=public
X-User-Id: 1
```

#### 获取表字段列表
```http
GET /api/v1/table-selection/datasource/1/tables/users/columns?database=mydb&schema=public
X-User-Id: 1
```

#### 保存表选择配置
```http
POST /api/v1/table-selection/1
Content-Type: application/json
X-User-Id: 1

[
  {
    "tableName": "users",
    "tableAlias": "u",
    "isPrimary": true,
    "selectedColumns": ["id", "name", "email", "created_at"]
  },
  {
    "tableName": "profiles",
    "tableAlias": "p",
    "isPrimary": false,
    "joinType": "LEFT",
    "joinCondition": "u.id = p.user_id",
    "selectedColumns": ["name as profile_name", "bio"]
  }
]
```

#### 生成SQL模板
```http
GET /api/v1/table-selection/1/sql-template
X-User-Id: 1
```

响应示例：
```json
{
  "sqlTemplate": "SELECT u.id,\n       u.name,\n       u.email,\n       u.created_at,\n       p.name as profile_name,\n       p.bio\nFROM users AS u\nLEFT JOIN profiles AS p ON u.id = p.user_id\nWHERE 1=1\n  -- 在此处添加查询条件，可使用参数：${paramName}"
}
```

## 使用流程

### 1. 创建API服务的完整流程

1. **选择数据源和表**: 使用表选择接口获取可用的表和字段
2. **配置表选择**: 选择需要的表和字段，配置JOIN条件
3. **生成SQL模板**: 系统自动生成基础SQL模板
4. **完善SQL**: 在模板基础上添加WHERE条件和参数
5. **创建服务**: 调用创建接口，状态为DRAFT
6. **测试验证**: 使用测试接口验证SQL和参数
7. **发布服务**: 发布为正式版本，状态变为PUBLISHED

### 2. SQL参数使用

在SQL中使用 `${参数名}` 格式定义参数：

```sql
SELECT u.id, u.name, u.email, p.bio
FROM users u
LEFT JOIN profiles p ON u.id = p.user_id  
WHERE u.status = ${status}
  AND u.created_at >= ${startDate}
  AND u.id IN (${userIds})
```

对应的参数配置：
```json
{
  "status": {
    "type": "string", 
    "required": true,
    "description": "用户状态"
  },
  "startDate": {
    "type": "date",
    "required": false, 
    "description": "开始日期"
  },
  "userIds": {
    "type": "array",
    "required": false,
    "description": "用户ID列表"
  }
}
```

### 3. 版本管理最佳实践

- **语义化版本**: 使用 major.minor.patch 格式
- **版本描述**: 每个版本都应有清晰的变更说明  
- **渐进发布**: 重要变更应该先在测试环境验证
- **版本对比**: 发布前对比版本差异确认变更
- **回滚策略**: 保持前一个稳定版本以备回滚

## 扩展点

### 1. 自定义数据源类型
实现 `DataSourceConnection` 接口支持新的数据源类型

### 2. 自定义参数处理
扩展 `EnhancedApiTestingService` 支持复杂参数类型

### 3. 自定义审计规则
扩展 `ApiServiceAuditLog` 添加业务相关的审计字段

### 4. 缓存策略
集成 Redis 等缓存中间件优化查询性能

### 5. 限流策略  
集成限流组件控制API调用频率

## 配置说明

在 `application.yml` 中可配置相关参数：

```yaml
auto-api:
  service:
    # 版本保留数量
    version-retention-count: 100
    # 审计日志保留天数
    audit-log-retention-days: 90
    # 默认缓存时长（秒）
    default-cache-duration: 300
    # 默认限流次数（每分钟）
    default-rate-limit: 1000
```

## 数据库表结构

核心表包括：
- `api_services`: API服务主表
- `api_service_versions`: API服务版本表
- `api_service_audit_logs`: 审计日志表
- `api_service_table_selections`: 表选择配置表

详细的DDL语句请参考 `schema.sql` 文件。