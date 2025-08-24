import { request } from '@umijs/max';

/** 测试草稿API POST /api/v1/testing/draft */
export async function testDraftApi(data: API.ApiTestRequest) {
  return request<{
    data: API.ApiTestResponse;
  }>('/api/v1/testing/draft', {
    method: 'POST',
    data,
  });
}

/** 测试已发布API POST /api/v1/testing/published/:apiServiceId */
export async function testPublishedApi(
  apiServiceId: number,
  data: {
    version?: string;
    parameters?: Record<string, any>;
  },
) {
  return request<{
    data: API.ApiTestResponse;
  }>(`/api/v1/testing/published/${apiServiceId}`, {
    method: 'POST',
    data,
  });
}

/** 验证SQL POST /api/v1/testing/validate-sql */
export async function validateSql(data: {
  dataSourceId: number;
  sql: string;
  parameters?: Record<string, any>;
}) {
  return request<{
    data: {
      valid: boolean;
      message: string;
      syntaxErrors?: {
        line: number;
        column: number;
        message: string;
      }[];
    };
  }>('/api/v1/testing/validate-sql', {
    method: 'POST',
    data,
  });
}

/** 解释SQL POST /api/v1/testing/explain-sql */
export async function explainSql(data: {
  dataSourceId: number;
  sql: string;
  parameters?: Record<string, any>;
}) {
  return request<{
    data: {
      success: boolean;
      executionPlan: {
        operation: string;
        cost?: number;
        rows?: number;
        details?: string;
      }[];
      estimatedCost?: number;
      estimatedRows?: number;
    };
  }>('/api/v1/testing/explain-sql', {
    method: 'POST',
    data,
  });
}

/** 批量测试API POST /api/v1/testing/batch/:apiServiceId */
export async function batchTestApi(
  apiServiceId: number,
  data: {
    testCases: {
      name: string;
      parameters: Record<string, any>;
    }[];
  },
) {
  return request<{
    data: {
      results: API.ApiTestResponse[];
      summary: {
        total: number;
        success: number;
        failed: number;
        averageTime: number;
      };
    };
  }>(`/api/v1/testing/batch/${apiServiceId}`, {
    method: 'POST',
    data,
  });
}