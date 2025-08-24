import { request } from '@umijs/max';

/** 获取数据源列表 GET /api/v2/datasources */
export async function getDataSources(params?: {
  current?: number;
  pageSize?: number;
  type?: API.DataSourceType;
  keyword?: string;
}) {
  return request<{
    data: {
      content: API.DataSource[];
      totalElements: number;
      totalPages: number;
      pageSize: number;
      pageNumber: number;
    };
  }>('/api/v2/datasources', {
    method: 'GET',
    params: {
      page: params?.current ? params.current - 1 : 0,
      size: params?.pageSize || 10,
      type: params?.type,
      keyword: params?.keyword,
    },
  });
}

/** 创建数据源 POST /api/v2/datasources */
export async function createDataSource(data: API.DataSourceCreateRequest) {
  return request<{
    data: API.DataSource;
  }>('/api/v2/datasources', {
    method: 'POST',
    data,
  });
}

/** 获取数据源详情 GET /api/v2/datasources/:id */
export async function getDataSource(id: number) {
  return request<{
    data: API.DataSource;
  }>(`/api/v2/datasources/${id}`, {
    method: 'GET',
  });
}

/** 更新数据源 PUT /api/v2/datasources/:id */
export async function updateDataSource(id: number, data: Partial<API.DataSourceCreateRequest>) {
  return request<{
    data: API.DataSource;
  }>(`/api/v2/datasources/${id}`, {
    method: 'PUT',
    data,
  });
}

/** 删除数据源 DELETE /api/v2/datasources/:id */
export async function deleteDataSource(id: number) {
  return request<{ success: boolean }>(`/api/v2/datasources/${id}`, {
    method: 'DELETE',
  });
}

/** 测试数据源连接 POST /api/v2/datasources/:id/test */
export async function testDataSourceConnection(id: number) {
  return request<{
    data: {
      connected: boolean;
      message: string;
      responseTime: number;
      lastTestTime: string;
    };
  }>(`/api/v2/datasources/${id}/test`, {
    method: 'POST',
  });
}

/** 测试数据源配置 POST /api/v2/datasources/test-config */
export async function testDataSourceConfig(data: API.DataSourceCreateRequest) {
  return request<{
    data: {
      connected: boolean;
      message: string;
      responseTime: number;
    };
  }>('/api/v2/datasources/test-config', {
    method: 'POST',
    data,
  });
}

/** 获取数据源元数据 GET /api/v2/datasources/:id/metadata */
export async function getDataSourceMetadata(id: number) {
  return request<{
    data: {
      databaseName: string;
      version: string;
      driverName: string;
      driverVersion: string;
    };
  }>(`/api/v2/datasources/${id}/metadata`, {
    method: 'GET',
  });
}

/** 获取数据库列表 GET /api/v2/datasources/:id/databases */
export async function getDatabases(id: number) {
  return request<{
    data: string[];
  }>(`/api/v2/datasources/${id}/databases`, {
    method: 'GET',
  });
}

/** 获取表列表 GET /api/v2/datasources/:id/tables */
export async function getTables(id: number, database?: string, schema?: string) {
  return request<{
    data: {
      tableName: string;
      tableType: string;
      comment?: string;
      schema?: string;
    }[];
  }>(`/api/v2/datasources/${id}/tables`, {
    method: 'GET',
    params: { database, schema },
  });
}

/** 获取表结构 GET /api/v2/datasources/:id/tables/:tableName/schema */
export async function getTableSchema(
  id: number,
  tableName: string,
  database?: string,
  schema?: string,
) {
  return request<{
    data: {
      tableName: string;
      columns: {
        columnName: string;
        dataType: string;
        nullable: boolean;
        defaultValue?: string;
        comment?: string;
        isPrimaryKey: boolean;
      }[];
      indexes: {
        indexName: string;
        columnNames: string[];
        unique: boolean;
      }[];
    };
  }>(`/api/v2/datasources/${id}/tables/${tableName}/schema`, {
    method: 'GET',
    params: { database, schema },
  });
}

/** 执行查询 POST /api/v2/datasources/:id/query */
export async function executeQuery(
  id: number,
  data: {
    query: string;
    parameters?: Record<string, any>;
    limit?: number;
  },
) {
  return request<{
    data: {
      success: boolean;
      data: any[];
      count: number;
      columns: string[];
      executionTime: number;
      errorMessage?: string;
    };
  }>(`/api/v2/datasources/${id}/query`, {
    method: 'POST',
    data,
  });
}

/** 验证查询 POST /api/v2/datasources/:id/query/validate */
export async function validateQuery(
  id: number,
  data: {
    query: string;
  },
) {
  return request<{
    data: {
      valid: boolean;
      message: string;
    };
  }>(`/api/v2/datasources/${id}/query/validate`, {
    method: 'POST',
    data,
  });
}

/** 获取支持的数据源类型 GET /api/v2/datasources/types */
export async function getSupportedDataSourceTypes() {
  return request<{
    data: API.DataSourceType[];
  }>('/api/v2/datasources/types', {
    method: 'GET',
  });
}