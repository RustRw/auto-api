import { defineConfig } from '@umijs/max';

export default defineConfig({
  routes: [
    {
      path: '/user/login',
      layout: false,
      component: './User/Login',
    },
    {
      path: '/welcome',
      name: '欢迎',
      icon: 'home',
      component: './Welcome',
      layout: false,
    },
    // 数据源管理路由
    {
      path: '/datasource',
      name: '数据源管理',
      icon: 'database',
      routes: [
        {
          path: '/datasource/list',
          name: '数据源列表',
          component: './DataSource/List',
          layout: false,
        },
        {
          path: '/datasource/create',
          name: '新建数据源',
          component: './DataSource/Create',
          hideInMenu: true,
          layout: false,
        },
        {
          path: '/datasource/edit/:id',
          name: '编辑数据源',
          component: './DataSource/Edit',
          hideInMenu: true,
          layout: false,
        },
      ],
    },
    // API服务管理路由
    {
      path: '/apiservice',
      name: 'API服务',
      icon: 'api',
      routes: [
        {
          path: '/apiservice/list',
          name: '服务列表',
          component: './ApiService/List',
          layout: false,
        },
        {
          path: '/apiservice/create',
          name: '新建服务',
          component: './ApiService/Create',
          hideInMenu: true,
        },
        {
          path: '/apiservice/edit/:id',
          name: '编辑服务',
          component: './ApiService/Edit',
          hideInMenu: true,
        },
        {
          path: '/apiservice/develop',
          name: '服务开发',
          component: './ApiService/Develop',
          layout: false,
        },
        {
          path: '/apiservice/testing',
          name: 'API测试',
          component: './ApiService/Testing',
        },
      ],
    },
    // 监控中心路由
    {
      path: '/monitoring',
      name: '监控中心',
      icon: 'monitor',
      routes: [
        {
          path: '/monitoring/overview',
          name: '监控概览',
          component: './Monitoring/Overview',
        },
        {
          path: '/monitoring/requests',
          name: '请求监控',
          component: './Monitoring/Requests',
        },
      ],
    },
    {
      path: '/',
      redirect: '/welcome',
    },
    {
      path: '*',
      layout: false,
      component: './404',
    },
  ],
  npmClient: 'npm',
  mfsu: false,
  proxy: {
    '/api/**': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      pathRewrite: { '^/api': '/api' },
    },
  },
});