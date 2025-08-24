package org.duqiu.fly.autoapi.test.service;

import lombok.RequiredArgsConstructor;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.dto.ApiTestResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApiTestingService {
    
    private final ApiTestManager apiTestManager;
    
    public ApiTestResponse executeTest(ApiTestRequest request, Long userId) {
        return apiTestManager.testDraftApi(request, userId);
    }
}