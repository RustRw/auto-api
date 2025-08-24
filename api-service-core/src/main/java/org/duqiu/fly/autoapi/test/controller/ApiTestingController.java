package org.duqiu.fly.autoapi.test.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.dto.ApiTestResponse;
import org.duqiu.fly.autoapi.test.service.ApiTestManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API测试控制器
 */
@RestController
@RequestMapping("/api/v1/testing")
@RequiredArgsConstructor
public class ApiTestingController {
    
    private final ApiTestManager testManager;
    
    /**
     * 测试草稿状态的API服务
     */
    @PostMapping("/draft")
    public ResponseEntity<ApiTestResponse> testDraftApi(
            @Valid @RequestBody ApiTestRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ApiTestResponse response = testManager.testDraftApi(request, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 测试已发布的API服务
     */
    @PostMapping("/published/{apiServiceId}")
    public ResponseEntity<ApiTestResponse> testPublishedApi(
            @PathVariable Long apiServiceId,
            @RequestParam(required = false) String version,
            @RequestBody Map<String, Object> parameters,
            @RequestHeader("X-User-Id") Long userId) {
        ApiTestResponse response = testManager.testPublishedApi(apiServiceId, version, parameters, userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 验证SQL语法
     */
    @PostMapping("/validate-sql")
    public ResponseEntity<Map<String, Object>> validateSql(
            @RequestParam Long dataSourceId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId) {
        
        String sql = (String) request.get("sql");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
        
        Map<String, Object> result = testManager.validateSql(dataSourceId, sql, parameters, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取SQL执行计划
     */
    @PostMapping("/explain-sql")
    public ResponseEntity<Map<String, Object>> explainSql(
            @RequestParam Long dataSourceId,
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId) {
        
        String sql = (String) request.get("sql");
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");
        
        Map<String, Object> result = testManager.explainSql(dataSourceId, sql, parameters, userId);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 批量测试API
     */
    @PostMapping("/batch/{apiServiceId}")
    public ResponseEntity<List<ApiTestResponse>> batchTestApi(
            @PathVariable Long apiServiceId,
            @RequestBody List<Map<String, Object>> parametersList,
            @RequestHeader("X-User-Id") Long userId) {
        List<ApiTestResponse> responses = testManager.batchTestApi(apiServiceId, parametersList, userId);
        return ResponseEntity.ok(responses);
    }
}