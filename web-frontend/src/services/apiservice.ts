import { request } from '@umijs/max';

/** 获取API服务列表 GET /api/services */
export async function getApiServices(params?: {
  current?: number;
  pageSize?: number;
  status?: API.ApiStatus;
  keyword?: string;
}) {
  return request<{
    data: {
      content: API.ApiService[];
      totalElements: number;
      totalPages: number;
      pageSize: number;
      pageNumber: number;
    };
  }>('/api/services', {
    method: 'GET',
    params: {
      page: params?.current ? params.current - 1 : 0,
      size: params?.pageSize || 10,
      status: params?.status,
      keyword: params?.keyword,
    },
  });
}

/** 创建API服务 POST /api/services */
export async function createApiService(data: API.ApiServiceCreateRequest) {
  return request<{
    data: API.ApiService;
  }>('/api/services', {
    method: 'POST',
    data,
  });
}

/** 获取API服务详情 GET /api/services/:id */
export async function getApiService(id: number) {
  return request<{
    data: API.ApiService;
  }>(`/api/services/${id}`, {
    method: 'GET',
  });
}

/** 更新API服务状态 PUT /api/services/:id/status */
export async function updateApiServiceStatus(id: number, status: API.ApiStatus) {
  return request<{
    data: API.ApiService;
  }>(`/api/services/${id}/status`, {
    method: 'PUT',
    data: { status },
  });
}

/** 删除API服务 DELETE /api/services/:id */
export async function deleteApiService(id: number) {
  return request<{ success: boolean }>(`/api/services/${id}`, {
    method: 'DELETE',
  });
}

// V1 API (核心模块)

/** 创建API服务 POST /api/v1/services */
export async function createApiServiceV1(data: API.ApiServiceCreateRequest) {
  return request<{
    data: API.ApiService;
  }>('/api/v1/services', {
    method: 'POST',
    data,
  });
}

/** 更新API服务 PUT /api/v1/services/:id */
export async function updateApiService(id: number, data: Partial<API.ApiServiceCreateRequest>) {
  return request<{
    data: API.ApiService;
  }>(`/api/v1/services/${id}`, {
    method: 'PUT',
    data,
  });
}

/** 发布API服务 POST /api/v1/services/:id/publish */
export async function publishApiService(
  id: number,
  data: {
    version: string;
    releaseNotes?: string;
  },
) {
  return request<{
    data: {
      id: number;
      version: string;
      publishedAt: string;
    };
  }>(`/api/v1/services/${id}/publish`, {
    method: 'POST',
    data,
  });
}

/** 取消发布API服务 POST /api/v1/services/:id/unpublish */
export async function unpublishApiService(id: number) {
  return request<{ success: boolean }>(`/api/v1/services/${id}/unpublish`, {
    method: 'POST',
  });
}

/** 获取API服务版本列表 GET /api/v1/services/:id/versions */
export async function getApiServiceVersions(
  id: number,
  params?: {
    current?: number;
    pageSize?: number;
  },
) {
  return request<{
    data: {
      content: {
        id: number;
        version: string;
        publishedAt: string;
        releaseNotes?: string;
        status: API.ApiStatus;
      }[];
      totalElements: number;
    };
  }>(`/api/v1/services/${id}/versions`, {
    method: 'GET',
    params: {
      page: params?.current ? params.current - 1 : 0,
      size: params?.pageSize || 10,
    },
  });
}

/** 比较API服务版本 GET /api/v1/services/:id/versions/compare */
export async function compareApiServiceVersions(
  id: number,
  version1: string,
  version2: string,
) {
  return request<{
    data: {
      version1: string;
      version2: string;
      differences: {
        field: string;
        oldValue: any;
        newValue: any;
      }[];
    };
  }>(`/api/v1/services/${id}/versions/compare`, {
    method: 'GET',
    params: { version1, version2 },
  });
}

/** 获取API服务审计日志 GET /api/v1/services/:id/audit-logs */
export async function getApiServiceAuditLogs(
  id: number,
  params?: {
    current?: number;
    pageSize?: number;
  },
) {
  return request<{
    data: {
      content: {
        id: number;
        action: string;
        details: string;
        createdAt: string;
        createdBy: number;
      }[];
      totalElements: number;
    };
  }>(`/api/v1/services/${id}/audit-logs`, {
    method: 'GET',
    params: {
      page: params?.current ? params.current - 1 : 0,
      size: params?.pageSize || 10,
    },
  });
}