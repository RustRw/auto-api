# Auto API Platform - 企业级API服务管理平台

## 🚀 项目概述

Auto API Platform 是一个全功能的企业级API服务管理平台，提供从数据源管理到API服务发布的完整解决方案。本项目采用前后端分离架构，后端基于Spring Boot，前端基于React + Ant Design Pro。

## 📁 项目结构

```
auto-api/
├── common-core/           # 公共核心模块
├── datasource-core/       # 数据源管理核心模块  
├── api-service-core/      # API服务管理核心模块
├── web-gateway/           # Web网关模块（后端主应用）
├── web-frontend/          # 前端应用（React + Ant Design Pro）
└── FRONTEND_SETUP.md      # 前端快速启动指南
```

## 🎯 核心功能特性

### 💻 前端功能 (React + Ant Design Pro)
- **🔐 用户认证** - 现代化登录/注册界面，JWT Token管理
- **💾 数据源管理** - 可视化数据源配置，支持多种数据库，实时连接测试
- **🛠 API服务开发** - 直观的API创建界面，版本管理，发布控制
- **💻 在线代码编辑器** - Monaco Editor集成，SQL语法高亮，实时执行
- **📊 监控仪表盘** - 实时性能监控，数据可视化，告警管理
- **📱 响应式设计** - 完美支持桌面、平板、移动设备

### ⚙️ 后端功能 (Spring Boot)

#### 1. 用户认证模块 (auth)
- 用户注册登录
- JWT令牌认证
- 角色权限管理

#### 2. 数据源管理模块 (datasource)
- 支持MySQL、Oracle、PostgreSQL、Elasticsearch等数据库
- 数据源连接配置和管理
- 连接测试功能

#### 3. API服务管理模块 (apiservice)
- API服务的创建、编辑、发布
- SQL到API的自动转换
- API状态管理（草稿、测试中、已发布、已禁用）

#### 4. 在线测试模块 (testing)
- API在线测试
- SQL语法验证
- 动态API端点执行

## 🔧 技术栈

### 后端技术
- **Spring Boot 3.5** - 企业级Java框架
- **Spring Security** - 安全认证框架  
- **Spring Data JPA** - 数据访问层
- **H2/MySQL/PostgreSQL** - 数据库支持
- **JWT** - 无状态认证
- **Gradle 8.10.2** - 构建工具
- **Java 21** - 最新LTS版本

### 前端技术
- **React 18** - 现代化前端框架
- **TypeScript** - 类型安全
- **Ant Design Pro** - 企业级UI解决方案
- **UmiJS** - 可插拔前端框架
- **Monaco Editor** - 专业代码编辑器
- **Ant Design Charts** - 数据可视化

## 🚀 快速开始

### 后端启动 (Spring Boot)

#### 1. 环境要求
- Java 21+
- Gradle 8.x 或使用项目自带的Gradle Wrapper

#### 2. 启动后端服务
```bash
./gradlew bootRun
```

#### 3. 访问后端
- 应用端口: http://localhost:8080
- H2控制台: http://localhost:8080/h2-console
- API接口: http://localhost:8080/api/*

### 前端启动 (React)

#### 1. 环境要求
- Node.js 16+
- npm 8+

#### 2. 安装前端依赖
```bash
cd web-frontend
npm install
```

#### 3. 启动前端服务
```bash
npm run dev
```

#### 4. 访问前端应用
- 前端地址: http://localhost:8000
- 自动代理后端API请求

### 🎯 完整启动流程

1. **启动后端**: `./gradlew bootRun`
2. **启动前端**: `cd web-frontend && npm run dev`  
3. **访问应用**: http://localhost:8000

### 4. 默认账户
系统启动后会自动创建以下测试账户：

**管理员账户:**
- 用户名: admin
- 密码: admin123

**普通用户账户:**
- 用户名: user  
- 密码: user123

## API接口说明

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册

### 数据源管理接口
- `POST /api/datasources` - 创建数据源
- `GET /api/datasources` - 获取用户数据源列表
- `GET /api/datasources/{id}` - 获取数据源详情
- `POST /api/datasources/{id}/test` - 测试数据源连接
- `DELETE /api/datasources/{id}` - 删除数据源

### API服务管理接口
- `POST /api/services` - 创建API服务
- `GET /api/services` - 获取用户API服务列表（支持分页）
- `GET /api/services/{id}` - 获取API服务详情
- `PUT /api/services/{id}/status` - 更新API服务状态
- `DELETE /api/services/{id}` - 删除API服务

### 测试接口
- `POST /api/testing/test` - 测试API服务
- `POST /api/testing/validate-sql` - 验证SQL语法

### 动态API端点
- `GET /api/dynamic/**` - 动态GET API调用
- `POST /api/dynamic/**` - 动态POST API调用

## 使用流程

1. **用户注册登录**: 创建账户并获取JWT令牌
2. **配置数据源**: 添加数据库连接信息
3. **创建API服务**: 配置SQL查询和API路径
4. **测试API**: 在线测试API功能
5. **发布API**: 将API状态设置为已发布
6. **调用API**: 通过动态端点调用已发布的API

## 数据库支持

### 关系型数据库
- MySQL: `jdbc:mysql://{host}:{port}/{database}`
- Oracle: `jdbc:oracle:thin:@{host}:{port}:{database}`
- PostgreSQL: `jdbc:postgresql://{host}:{port}/{database}`

### 非关系型数据库
- Elasticsearch: `http://{host}:{port}`

## 配置说明

主要配置在 `application.properties` 中：

```properties
# 数据库配置（开发环境使用H2）
spring.datasource.url=jdbc:h2:mem:autoapi
spring.h2.console.enabled=true

# JWT配置
app.jwt.secret=mySecretKey
app.jwt.expiration=86400000

# 日志配置
logging.level.org.duqiu.fly.autoapi=DEBUG
```

## 项目结构

```
src/main/java/org/duqiu/fly/autoapi/
├── auth/           # 用户认证模块
├── datasource/     # 数据源管理模块
├── apiservice/     # API服务管理模块
├── testing/        # 在线测试模块
├── common/         # 公共模块
└── config/         # 配置类
```

## 注意事项

1. 默认使用H2内存数据库，重启后数据会丢失
2. 生产环境需要配置真实的数据库连接
3. JWT密钥需要在生产环境中修改为安全的值
4. API调用需要在请求头中包含JWT令牌: `Authorization: Bearer {token}`

## 开发计划

- [ ] 添加API文档自动生成
- [ ] 实现API缓存机制
- [ ] 添加API调用限流功能
- [ ] 支持更多数据库类型
- [ ] 添加API监控和统计功能