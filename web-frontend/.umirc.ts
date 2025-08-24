import { defineConfig } from '@umijs/max';

export default defineConfig({
  antd: {},
  access: {},
  model: {},
  initialState: {},
  request: {},
  layout: {
    title: 'Auto API Platform',
    locale: true,
  },
  routes: [
    {
      path: '/user',
      layout: false,
      routes: [
        {
          name: 'login',
          path: '/user/login',
          component: './User/Login',
        },
        {
          name: 'register',
          path: '/user/register',
          component: './User/Register',
        },
      ],
    },
    {
      path: '/welcome',
      name: 'welcome',
      icon: 'smile',
      component: './Welcome',
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      icon: 'dashboard',
      component: './Dashboard',
    },
    {
      path: '/datasource',
      name: 'datasource',
      icon: 'database',
      routes: [
        {
          path: '/datasource',
          redirect: '/datasource/list',
        },
        {
          name: 'list',
          path: '/datasource/list',
          component: './DataSource/List',
        },
        {
          name: 'create',
          path: '/datasource/create',
          component: './DataSource/Create',
        },
        {
          name: 'edit',
          path: '/datasource/edit/:id',
          component: './DataSource/Edit',
          hideInMenu: true,
        },
      ],
    },
    {
      path: '/apiservice',
      name: 'apiservice',
      icon: 'api',
      routes: [
        {
          path: '/apiservice',
          redirect: '/apiservice/list',
        },
        {
          name: 'list',
          path: '/apiservice/list',
          component: './ApiService/List',
        },
        {
          name: 'create',
          path: '/apiservice/create',
          component: './ApiService/Create',
        },
        {
          name: 'edit',
          path: '/apiservice/edit/:id',
          component: './ApiService/Edit',
          hideInMenu: true,
        },
        {
          name: 'testing',
          path: '/apiservice/testing',
          component: './ApiService/Testing',
        },
      ],
    },
    {
      path: '/monitoring',
      name: 'monitoring',
      icon: 'monitor',
      routes: [
        {
          path: '/monitoring',
          redirect: '/monitoring/overview',
        },
        {
          name: 'overview',
          path: '/monitoring/overview',
          component: './Monitoring/Overview',
        },
        {
          name: 'requests',
          path: '/monitoring/requests',
          component: './Monitoring/Requests',
        },
        {
          name: 'performance',
          path: '/monitoring/performance',
          component: './Monitoring/Performance',
        },
      ],
    },
    {
      path: '/settings',
      name: 'settings',
      icon: 'setting',
      routes: [
        {
          path: '/settings',
          redirect: '/settings/profile',
        },
        {
          name: 'profile',
          path: '/settings/profile',
          component: './Settings/Profile',
        },
        {
          name: 'security',
          path: '/settings/security',
          component: './Settings/Security',
        },
        {
          name: 'tenant',
          path: '/settings/tenant',
          component: './Settings/Tenant',
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
  tailwindcss: {},
  locale: {
    default: 'zh-CN',
    antd: true,
    title: true,
    baseNavigator: true,
    baseSeparator: '-',
  },
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      pathRewrite: { '^/api': '/api' },
    },
  },
  mfsu: {
    strategy: 'normal',
  },
  requestRecord: {},
});