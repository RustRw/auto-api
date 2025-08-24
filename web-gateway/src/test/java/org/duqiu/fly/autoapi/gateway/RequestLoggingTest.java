package org.duqiu.fly.autoapi.gateway;

import org.duqiu.fly.autoapi.AutoApiApplication;
import org.duqiu.fly.autoapi.gateway.dto.ServiceRequestLog;
import org.duqiu.fly.autoapi.gateway.service.ServiceRequestLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 请求日志测试
 * 测试服务请求日志功能
 */
@SpringBootTest(classes = AutoApiApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class RequestLoggingTest {

    @Autowired
    private ServiceRequestLogService requestLogService;

    @BeforeEach
    void setUp() {
        // 清空之前的日志
        requestLogService.clearRequestLogs();
    }

    @Test
    void testCreateAndLogRequest() {
        // 创建请求日志
        ServiceRequestLog log = requestLogService.createRequestLog(
                "USER_QUERY",
                1L,
                "/api/users/1"
        );

        // 设置请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 1);
        params.put("includeDetails", true);
        log.setRequestParameters(params);

        // 设置执行信息
        log.setExecutedSqlScript("SELECT * FROM users WHERE id = 1");
        log.setRecordCount(1);
        log.setResponseSizeBytes(150L);
        log.setClientIpAddress("192.168.1.100");
        log.setUserAgent("TestClient/1.0");
        log.setUserId("test-user");
        log.setUsername("Test User");

        // 标记请求完成
        log.markRequestCompleted(true);

        // 记录日志
        requestLogService.logServiceRequest(log);

        // 验证日志被记录
        ServiceRequestLog retrievedLog = requestLogService.getRequestLog(log.getLogId());
        assertNotNull(retrievedLog, "应该能找到记录的日志");
        assertEquals(log.getLogId(), retrievedLog.getLogId());
        assertEquals("USER_QUERY", retrievedLog.getServiceType());
        assertEquals(1L, retrievedLog.getServiceId());
        assertEquals("/api/users/1", retrievedLog.getServiceUrl());
        assertTrue(retrievedLog.getIsSuccess());
        assertEquals(1, retrievedLog.getRecordCount());
        assertEquals(150L, retrievedLog.getResponseSizeBytes());
    }

    @Test
    void testLogFailedRequest() {
        // 创建请求日志
        ServiceRequestLog log = requestLogService.createRequestLog(
                "USER_QUERY",
                1L,
                "/api/users/999"
        );

        // 设置错误信息
        log.setError("用户不存在", "java.sql.SQLException: No user found with id 999");
        log.markRequestCompleted(false);

        // 记录日志
        requestLogService.logServiceRequest(log);

        // 验证错误日志
        ServiceRequestLog retrievedLog = requestLogService.getRequestLog(log.getLogId());
        assertNotNull(retrievedLog);
        assertFalse(retrievedLog.getIsSuccess());
        assertEquals("用户不存在", retrievedLog.getErrorMessage());
        assertNotNull(retrievedLog.getErrorStackTrace());
    }

    @Test
    void testGetLogsByServiceId() {
        // 创建多个服务的日志
        ServiceRequestLog log1 = createTestLog("USER_QUERY", 1L, "/api/users/1", true);
        ServiceRequestLog log2 = createTestLog("USER_QUERY", 1L, "/api/users/2", true);
        ServiceRequestLog log3 = createTestLog("PRODUCT_QUERY", 2L, "/api/products/1", true);

        requestLogService.logServiceRequest(log1);
        requestLogService.logServiceRequest(log2);
        requestLogService.logServiceRequest(log3);

        // 获取特定服务的日志
        List<ServiceRequestLog> userLogs = requestLogService.getRequestLogsByServiceId(1L);
        assertEquals(2, userLogs.size(), "应该找到2个用户服务的日志");

        List<ServiceRequestLog> productLogs = requestLogService.getRequestLogsByServiceId(2L);
        assertEquals(1, productLogs.size(), "应该找到1个产品服务的日志");

        List<ServiceRequestLog> nonExistentLogs = requestLogService.getRequestLogsByServiceId(999L);
        assertTrue(nonExistentLogs.isEmpty(), "不存在的服务应该返回空列表");
    }

    @Test
    void testGetLogsByTimeRange() {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(5);

        // 创建日志
        ServiceRequestLog log1 = createTestLog("USER_QUERY", 1L, "/api/users/1", true);
        ServiceRequestLog log2 = createTestLog("USER_QUERY", 1L, "/api/users/2", true);

        requestLogService.logServiceRequest(log1);
        requestLogService.logServiceRequest(log2);

        LocalDateTime endTime = LocalDateTime.now().plusMinutes(1);

        // 获取时间范围内的日志
        List<ServiceRequestLog> logsInRange = requestLogService.getRequestLogsByTimeRange(
                startTime, endTime
        );

        assertTrue(logsInRange.size() >= 2, "应该找到时间范围内的日志");

        // 测试时间范围外
        List<ServiceRequestLog> logsOutsideRange = requestLogService.getRequestLogsByTimeRange(
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusMinutes(10)
        );
        assertTrue(logsOutsideRange.isEmpty(), "时间范围外应该没有日志");
    }

    @Test
    void testServiceStatistics() {
        // 创建成功和失败的日志
        ServiceRequestLog successLog1 = createTestLog("USER_QUERY", 1L, "/api/users/1", true);
        ServiceRequestLog successLog2 = createTestLog("USER_QUERY", 1L, "/api/users/2", true);
        ServiceRequestLog failedLog = createTestLog("USER_QUERY", 1L, "/api/users/999", false);

        requestLogService.logServiceRequest(successLog1);
        requestLogService.logServiceRequest(successLog2);
        requestLogService.logServiceRequest(failedLog);

        // 获取统计信息
        var stats = requestLogService.getServiceStatistics();

        assertEquals(3L, stats.get("totalRequests"));
        assertEquals(2L, stats.get("successCount"));
        assertEquals(1L, stats.get("failedCount"));
        assertTrue(stats.get("successRate").toString().contains("66.67"));
        assertNotNull(stats.get("averageResponseTime"));
    }

    @Test
    void testLogWithDetailedTiming() {
        ServiceRequestLog log = requestLogService.createRequestLog(
                "DETAILED_QUERY",
                1L,
                "/api/users/1"
        );

        // 设置详细的时间信息
        log.setConnectionTimeMs(50L);
        log.setSqlExecutionTimeMs(120L);
        log.setDataProcessingTimeMs(30L);

        log.markRequestCompleted(true);
        requestLogService.logServiceRequest(log);

        ServiceRequestLog retrievedLog = requestLogService.getRequestLog(log.getLogId());
        assertEquals(50L, retrievedLog.getConnectionTimeMs());
        assertEquals(120L, retrievedLog.getSqlExecutionTimeMs());
        assertEquals(30L, retrievedLog.getDataProcessingTimeMs());

        // 总执行时间应该等于各个阶段之和
        assertTrue(retrievedLog.getExecutionTimeMs() >= 200L);
    }

    @Test
    void testLogWithHeadersAndTrace() {
        ServiceRequestLog log = requestLogService.createRequestLog(
                "TRACED_QUERY",
                1L,
                "/api/users/1"
        );

        // 设置请求头和追踪信息
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer token123");
        log.setRequestHeaders(headers);

        log.setTraceId("trace-123456");
        log.setResponseStatusCode(200);

        log.markRequestCompleted(true);
        requestLogService.logServiceRequest(log);

        ServiceRequestLog retrievedLog = requestLogService.getRequestLog(log.getLogId());
        assertEquals(2, retrievedLog.getRequestHeaders().size());
        assertEquals("trace-123456", retrievedLog.getTraceId());
        assertEquals(200, retrievedLog.getResponseStatusCode());
    }

    @Test
    void testClearLogs() {
        // 创建一些日志
        ServiceRequestLog log1 = createTestLog("USER_QUERY", 1L, "/api/users/1", true);
        ServiceRequestLog log2 = createTestLog("USER_QUERY", 1L, "/api/users/2", true);

        requestLogService.logServiceRequest(log1);
        requestLogService.logServiceRequest(log2);

        assertEquals(2, requestLogService.getRequestLogCount());

        // 清空日志
        requestLogService.clearRequestLogs();

        assertEquals(0, requestLogService.getRequestLogCount());
        assertTrue(requestLogService.getAllRequestLogs().isEmpty());
    }

    private ServiceRequestLog createTestLog(String serviceType, Long serviceId, String serviceUrl, boolean success) {
        ServiceRequestLog log = requestLogService.createRequestLog(serviceType, serviceId, serviceUrl);
        
        Map<String, Object> params = new HashMap<>();
        params.put("testParam", "value");
        log.setRequestParameters(params);
        
        log.setExecutedSqlScript("SELECT * FROM test_table");
        log.setRecordCount(1);
        log.setResponseSizeBytes(100L);
        log.setClientIpAddress("127.0.0.1");
        log.setUserAgent("Test");
        
        if (!success) {
            log.setError("Test error", "Test stack trace");
        }
        
        log.markRequestCompleted(success);
        return log;
    }
}