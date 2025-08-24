# Auto API Platform - 前端项目

企业级API服务管理平台的前端应用，基于React + Ant Design Pro构建。

## 🚀 技术栈

- **React 18** - 现代化前端框架
- **TypeScript** - 类型安全
- **Ant Design Pro** - 企业级UI解决方案
- **UmiJS** - 可插拔的企业级前端应用框架
- **Monaco Editor** - VS Code同款代码编辑器
- **Ant Design Charts** - 数据可视化

## 📋 主要功能

### 🔐 用户管理
- 用户登录/注册
- JWT Token认证
- 权限控制

### 💾 数据源管理
- 支持多种数据库类型（MySQL、PostgreSQL、Oracle、H2、Elasticsearch）
- 数据源连接测试
- 可视化数据源配置
- 元数据查询

### 🛠 API服务管理
- 可视化API创建
- SQL到RESTful API转换
- API版本管理
- 服务发布控制

### 💻 在线代码编辑
- Monaco Editor集成
- SQL语法高亮
- 实时语法检查
- 执行计划分析

### 📊 监控告警
- 实时性能监控
- API调用统计
- 错误率追踪
- 可视化图表展示

### ⚡ 在线测试
- API在线测试
- SQL验证
- 参数化测试
- 结果可视化

## 🏗 项目结构

```
web-frontend/
├── src/
│   ├── components/          # 公共组件
│   ├── pages/              # 页面组件
│   │   ├── User/           # 用户认证页面
│   │   ├── Dashboard/      # 工作台
│   │   ├── DataSource/     # 数据源管理
│   │   ├── ApiService/     # API服务管理
│   │   ├── Monitoring/     # 监控页面
│   │   └── Settings/       # 设置页面
│   ├── services/           # API服务
│   ├── utils/              # 工具函数
│   └── app.tsx            # 应用入口
├── config/                 # 配置文件
└── package.json
```

## 🔧 开发环境设置

### 环境要求
- Node.js >= 16.0.0
- npm >= 8.0.0

### 安装依赖
```bash
cd web-frontend
npm install
```

### 启动开发服务器
```bash
npm run dev
```

应用将在 http://localhost:8000 启动

### 构建生产版本
```bash
npm run build
```

### 代码检查
```bash
npm run lint
```

## 📡 API代理配置

开发环境下，前端请求会代理到后端服务：

```typescript
// .umirc.ts
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    pathRewrite: { '^/api': '/api' },
  },
}
```

## 🎨 主题配置

项目使用Ant Design的主题系统：

```typescript
// config/defaultSettings.ts
const Settings = {
  navTheme: 'light',
  colorPrimary: '#1890ff',
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  // ...
};
```

## 📱 响应式设计

所有页面都支持响应式设计，适配：
- 桌面端 (>= 1200px)
- 平板端 (768px - 1199px) 
- 移动端 (< 768px)

## 🔒 权限控制

基于路由的权限控制，支持：
- 页面级权限
- 按钮级权限
- 数据级权限

## 🌐 国际化

支持中英文双语：
- 中文（简体）- 默认
- English

## 📈 性能优化

- 代码分割
- 组件懒加载
- 图片懒加载
- Bundle分析
- 缓存策略

## 🧪 测试

```bash
# 单元测试
npm run test

# 组件测试
npm run test:component

# E2E测试
npm run test:e2e
```

## 📦 部署

### Docker部署
```bash
docker build -t auto-api-frontend .
docker run -p 80:80 auto-api-frontend
```

### 静态部署
构建后将`dist`目录部署到任何静态服务器。

## 🤝 开发指南

### 添加新页面
1. 在`src/pages`下创建页面组件
2. 在`.umirc.ts`中添加路由配置
3. 更新菜单配置

### 添加新API
1. 在`src/services`下创建服务文件
2. 定义TypeScript类型
3. 使用`request`发起HTTP请求

### 组件开发规范
- 使用TypeScript
- 遵循Ant Design设计规范
- 组件命名使用PascalCase
- 文件命名使用camelCase

## 🐛 常见问题

### 启动失败
1. 检查Node.js版本
2. 清理node_modules：`rm -rf node_modules && npm install`
3. 检查端口占用

### API请求失败
1. 确认后端服务已启动
2. 检查代理配置
3. 验证API路径

## 📞 技术支持

如有问题，请提交Issue或联系开发团队。

## 📄 许可证

MIT License