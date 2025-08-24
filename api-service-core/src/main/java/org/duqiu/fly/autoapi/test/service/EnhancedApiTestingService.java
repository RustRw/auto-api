package org.duqiu.fly.autoapi.test.service;

import lombok.RequiredArgsConstructor;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.dto.ApiTestResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnhancedApiTestingService {
    
    private final ApiTestManager apiTestManager;
    
    public ApiTestResponse testApi(ApiTestRequest request, Long userId) {
        return apiTestManager.testDraftApi(request, userId);
    }
    
    public ApiTestResponse testDraftApi(ApiTestRequest request, Long userId) {
        return apiTestManager.testDraftApi(request, userId);
    }
    
    public ApiTestResponse testPublishedApi(Long apiServiceId, String version, Map<String, Object> parameters, Long userId) {
        return apiTestManager.testPublishedApi(apiServiceId, version, parameters, userId);
    }
    
    public List<ApiTestResponse> batchTestApi(Long apiServiceId, List<Map<String, Object>> parametersList, Long userId) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    public Map<String, Object> validateSql(Long dataSourceId, String sql, Map<String, Object> parameters, Long userId) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
    
    public Map<String, Object> explainSql(Long dataSourceId, String sql, Map<String, Object> parameters, Long userId) {
        // Stub implementation for testing
        throw new UnsupportedOperationException("Method not implemented");
    }
}