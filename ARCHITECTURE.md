# Auto-API 多模块架构说明

## 项目概述

Auto-API 是一个在线API开发和发布服务平台，支持多种数据源类型的统一管理和API服务的快速创建、发布与测试。

## 模块架构

本项目采用多模块Gradle构建，分为以下四个核心模块：

```
auto-api/
├── common/                 # 公共模块
├── datasource-api/        # 数据源管理模块  
├── apiservice-core/       # API服务核心模块
├── web-gateway/          # Web网关模块
├── settings.gradle       # 模块配置
└── build.gradle         # 根项目构建配置
```

## 模块详细说明

### 1. common - 公共模块

**职责：** 提供项目通用的基础设施和工具类

**主要内容：**
- 通用DTO类：`Result`, `PageResult`
- 枚举类：`ApiStatus` 等
- 工具类和常量定义
- 异常处理类
- 基础配置类

**依赖关系：** 无外部模块依赖，被其他所有模块依赖

**关键类：**
- `org.duqiu.fly.autoapi.common.dto.Result` - 统一响应结果包装
- `org.duqiu.fly.autoapi.common.dto.PageResult` - 分页结果包装
- `org.duqiu.fly.autoapi.common.enums.ApiStatus` - API状态枚举

### 2. datasource-api - 数据源管理模块

**职责：** 数据源的统一管理、连接池管理和多种数据库类型支持

**支持的数据源类型：**
- **JDBC数据库：** MySQL, PostgreSQL, Oracle, ClickHouse, StarRocks, TDengine
- **NoSQL数据库：** MongoDB, Elasticsearch, NebulaGraph  
- **消息队列：** Kafka
- **HTTP API：** 外部REST接口

**核心架构特性：**
- 工厂模式创建数据源连接
- 统一抽象接口 `DataSourceConnection`
- HikariCP连接池管理
- 多版本驱动支持
- 协议分类管理（JDBC/HTTP/Native）

**依赖关系：**
- 依赖：`common`
- Spring Boot Starter (Web, JPA, Validation)
- 各类数据库驱动包
- HikariCP连接池

**关键类：**
- `DataSourceType` - 数据源类型枚举定义
- `UnifiedDataSourceFactory` - 数据源工厂类
- `DataSourceConnection` - 统一连接接口
- `EnhancedDataSourceService` - 数据源服务类

### 3. apiservice-core - API服务核心模块

**职责：** API服务的创建、管理和业务逻辑处理

**主要功能：**
- API服务创建和配置
- API生命周期管理
- 业务规则验证
- 服务状态管理

**依赖关系：**
- 依赖：`common`, `datasource-api`
- Spring Boot Starter (Web, JPA, Validation)
- Jackson JSON处理

**关键类：**
- `ApiServiceService` - API服务业务逻辑
- `ApiServiceCreateRequest` - API服务创建请求DTO
- `ApiServiceResponse` - API服务响应DTO

### 4. web-gateway - Web网关模块

**职责：** HTTP请求接入、路由分发和用户认证

**主要功能：**
- RESTful API端点暴露
- 用户认证和授权
- 请求参数验证
- 异常处理和响应包装

**依赖关系：**
- 依赖：`common`, `apiservice-core`, `datasource-api`
- Spring Boot Starter (Web, Security)
- Spring Security认证框架

**关键类：**
- `AutoApiApplication` - 主启动类
- `ApiServiceController` - API服务控制器
- `DataSourceController` - 数据源控制器

## 技术栈

- **Java：** 21
- **Spring Boot：** 3.5
- **构建工具：** Gradle 8.10.2
- **数据持久化：** Spring Data JPA
- **安全框架：** Spring Security  
- **连接池：** HikariCP
- **JSON处理：** Jackson

## 构建和运行

### 构建整个项目
```bash
./gradlew build
```

### 运行应用
```bash
./gradlew :web-gateway:bootRun
```

### 模块独立构建
```bash
./gradlew :datasource-api:build
./gradlew :apiservice-core:build
```

## 模块间通信

1. **web-gateway** → **apiservice-core** → **datasource-api** → **common**
2. 通过Spring的依赖注入实现模块间服务调用
3. 统一的异常处理和响应格式
4. 组件扫描配置：`@SpringBootApplication(scanBasePackages = "org.duqiu.fly.autoapi")`

## 扩展性设计

- **数据源扩展：** 通过 `DataSourceType` 枚举添加新的数据源类型
- **协议扩展：** 在工厂类中添加新的协议处理逻辑
- **API功能扩展：** 在 `apiservice-core` 模块添加新的业务服务
- **认证扩展：** 在 `web-gateway` 模块扩展认证策略

## 最佳实践

1. **模块职责单一：** 每个模块专注特定领域功能
2. **依赖方向清晰：** 避免循环依赖，保持单向依赖关系
3. **接口抽象：** 使用接口定义模块间契约
4. **异常统一处理：** 在web层统一处理和包装异常
5. **配置外置：** 重要配置通过application.properties管理