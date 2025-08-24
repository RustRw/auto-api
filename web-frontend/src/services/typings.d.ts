declare namespace API {
  type CurrentUser = {
    id?: number;
    username?: string;
    email?: string;
    avatar?: string;
    name?: string;
    signature?: string;
    title?: string;
    group?: string;
    tags?: { key?: string; label?: string }[];
    notifyCount?: number;
    unreadCount?: number;
    country?: string;
    access?: string;
    geographic?: {
      province?: { label?: string; key?: string };
      city?: { label?: string; key?: string };
    };
    address?: string;
    phone?: string;
  };

  type LoginResult = {
    status?: string;
    type?: string;
    currentAuthority?: string;
    token?: string;
  };

  type PageParams = {
    current?: number;
    pageSize?: number;
  };

  type RuleListItem = {
    key?: number;
    disabled?: boolean;
    href?: string;
    avatar?: string;
    name?: string;
    owner?: string;
    desc?: string;
    callNo?: number;
    status?: number;
    updatedAt?: string;
    createdAt?: string;
    progress?: number;
  };

  type RuleList = {
    data?: RuleListItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };

  type FakeCaptcha = {
    code?: number;
    status?: string;
  };

  type LoginParams = {
    username?: string;
    password?: string;
    autoLogin?: boolean;
    type?: string;
  };

  type RegisterParams = {
    username?: string;
    password?: string;
    email?: string;
    confirmPassword?: string;
  };

  type ErrorResponse = {
    /** 业务约定的错误码 */
    errorCode: string;
    /** 业务上的错误信息 */
    errorMessage?: string;
    /** 业务上的请求是否成功 */
    success?: boolean;
  };

  type NoticeIconList = {
    data?: NoticeIconItem[];
    /** 列表的内容总数 */
    total?: number;
    success?: boolean;
  };

  type NoticeIconItemType = 'notification' | 'message' | 'event';

  type NoticeIconItem = {
    id?: string;
    extra?: string;
    key?: string;
    read?: boolean;
    avatar?: string;
    title?: string;
    status?: string;
    datetime?: string;
    description?: string;
    type?: NoticeIconItemType;
  };

  // 数据源相关类型定义
  type DataSourceType = 'MYSQL' | 'POSTGRESQL' | 'ORACLE' | 'H2' | 'ELASTICSEARCH';

  type DataSource = {
    id: number;
    name: string;
    description?: string;
    type: DataSourceType;
    host: string;
    port: number;
    database?: string;
    username: string;
    password?: string;
    enabled: boolean;
    createdAt: string;
    updatedAt: string;
  };

  type DataSourceCreateRequest = {
    name: string;
    description?: string;
    type: DataSourceType;
    host: string;
    port: number;
    database?: string;
    username: string;
    password: string;
    maxPoolSize?: number;
    connectionTimeout?: number;
    testQuery?: string;
  };

  // API服务相关类型定义
  type ApiStatus = 'DRAFT' | 'TESTING' | 'PUBLISHED' | 'DISABLED';
  type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

  type ApiService = {
    id: number;
    name: string;
    description?: string;
    path: string;
    method: HttpMethod;
    dataSourceId: number;
    sqlContent: string;
    requestParams?: string;
    responseExample?: string;
    status: ApiStatus;
    enabled: boolean;
    cacheEnabled: boolean;
    cacheDuration?: number;
    rateLimit?: number;
    createdAt: string;
    updatedAt: string;
  };

  type ApiServiceCreateRequest = {
    name: string;
    description?: string;
    path: string;
    method: HttpMethod;
    dataSourceId: number;
    sqlContent: string;
    requestParams?: string;
    cacheEnabled: boolean;
    cacheDuration?: number;
    rateLimit?: number;
  };

  // 测试相关类型定义
  type ApiTestRequest = {
    dataSourceId: number;
    sqlContent: string;
    parameters?: Record<string, any>;
  };

  type ApiTestResponse = {
    success: boolean;
    data?: any;
    message?: string;
    executionTime: number;
    statusCode: number;
    errorDetail?: string;
    testTime: string;
    recordCount?: number;
  };
}