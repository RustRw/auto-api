package org.duqiu.fly.autoapi.gateway.service;

import org.duqiu.fly.autoapi.gateway.dto.ServiceRequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 服务请求日志服务
 * 负责记录和管理API服务的请求日志
 */
@Service
public class ServiceRequestLogService {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceRequestLogService.class);
    
    /**
     * 日志存储（在实际生产环境中应该使用数据库或日志系统）
     */
    private final Map<String, ServiceRequestLog> requestLogs = new ConcurrentHashMap<>();
    
    /**
     * 异步日志处理线程池
     */
    private ThreadPoolExecutor logExecutor;
    
    /**
     * 初始化服务
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing Service Request Log Service...");
        
        // 创建异步日志处理线程池
        logExecutor = new ThreadPoolExecutor(
            2, // 核心线程数
            5, // 最大线程数
            60, // 空闲线程存活时间
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000), // 队列容量
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
        
        logger.info("Service Request Log Service initialized");
    }
    
    /**
     * 记录服务请求日志
     * @param log 请求日志对象
     */
    public void logServiceRequest(ServiceRequestLog log) {
        if (log == null) {
            return;
        }
        
        // 异步处理日志记录
        logExecutor.submit(() -> {
            try {
                processLog(log);
            } catch (Exception e) {
                logger.error("Failed to process service request log: {}", log.getLogId(), e);
            }
        });
    }
    
    /**
     * 处理日志记录
     * @param log 请求日志对象
     */
    private void processLog(ServiceRequestLog log) {
        // 存储日志
        requestLogs.put(log.getLogId(), log);
        
        // 输出到控制台（在实际生产环境中应该写入数据库或日志文件）
        String logMessage = buildLogMessage(log);
        
        if (Boolean.TRUE.equals(log.getIsSuccess())) {
            logger.info(logMessage);
        } else {
            logger.error("{} - Error: {}", logMessage, log.getErrorMessage());
        }
    }
    
    /**
     * 构建日志消息
     * @param log 请求日志对象
     * @return 格式化后的日志消息
     */
    private String buildLogMessage(ServiceRequestLog log) {
        return String.format(
            "ServiceRequest[%s] - Type: %s, Service: %d, URL: %s, Method: %s, " +
            "Version: %s, Status: %s, Time: %dms, Records: %d, Size: %dB, " +
            "Client: %s, User: %s",
            log.getLogId(),
            log.getServiceType(),
            log.getServiceId(),
            log.getServiceUrl(),
            log.getHttpMethod(),
            log.getServiceVersion(),
            log.getIsSuccess() ? "SUCCESS" : "FAILED",
            log.getExecutionTimeMs(),
            log.getRecordCount() != null ? log.getRecordCount() : 0,
            log.getResponseSizeBytes() != null ? log.getResponseSizeBytes() : 0,
            log.getClientIpAddress(),
            log.getUsername()
        );
    }
    
    /**
     * 创建新的服务请求日志
     * @param serviceType 服务类型
     * @param serviceId 服务ID
     * @param serviceUrl 服务URL
     * @return 请求日志对象
     */
    public ServiceRequestLog createRequestLog(String serviceType, Long serviceId, String serviceUrl) {
        return new ServiceRequestLog(serviceType, serviceId, serviceUrl);
    }
    
    /**
     * 根据日志ID获取请求日志
     * @param logId 日志ID
     * @return 请求日志对象
     */
    public ServiceRequestLog getRequestLog(String logId) {
        return requestLogs.get(logId);
    }
    
    /**
     * 根据服务ID获取请求日志列表
     * @param serviceId 服务ID
     * @return 请求日志列表
     */
    public List<ServiceRequestLog> getRequestLogsByServiceId(Long serviceId) {
        List<ServiceRequestLog> result = new ArrayList<>();
        requestLogs.values().stream()
            .filter(log -> serviceId.equals(log.getServiceId()))
            .forEach(result::add);
        return result;
    }
    
    /**
     * 根据时间范围获取请求日志列表
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 请求日志列表
     */
    public List<ServiceRequestLog> getRequestLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        List<ServiceRequestLog> result = new ArrayList<>();
        requestLogs.values().stream()
            .filter(log -> log.getRequestStartTime() != null &&
                          log.getRequestStartTime().isAfter(startTime) &&
                          log.getRequestStartTime().isBefore(endTime))
            .forEach(result::add);
        return result;
    }
    
    /**
     * 获取所有请求日志
     * @return 请求日志列表
     */
    public List<ServiceRequestLog> getAllRequestLogs() {
        return new ArrayList<>(requestLogs.values());
    }
    
    /**
     * 获取请求日志数量
     * @return 日志数量
     */
    public int getRequestLogCount() {
        return requestLogs.size();
    }
    
    /**
     * 清空请求日志
     */
    public void clearRequestLogs() {
        requestLogs.clear();
        logger.info("All request logs cleared");
    }
    
    /**
     * 获取服务统计信息
     * @return 服务统计映射
     */
    public Map<String, Object> getServiceStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        long totalRequests = requestLogs.size();
        long successCount = requestLogs.values().stream()
            .filter(log -> Boolean.TRUE.equals(log.getIsSuccess()))
            .count();
        long failedCount = totalRequests - successCount;
        
        double successRate = totalRequests > 0 ? (double) successCount / totalRequests * 100 : 0;
        
        stats.put("totalRequests", totalRequests);
        stats.put("successCount", successCount);
        stats.put("failedCount", failedCount);
        stats.put("successRate", String.format("%.2f%%", successRate));
        stats.put("averageResponseTime", calculateAverageResponseTime());
        
        return stats;
    }
    
    /**
     * 计算平均响应时间
     * @return 平均响应时间（毫秒）
     */
    private double calculateAverageResponseTime() {
        return requestLogs.values().stream()
            .filter(log -> log.getExecutionTimeMs() != null && log.getExecutionTimeMs() > 0)
            .mapToLong(ServiceRequestLog::getExecutionTimeMs)
            .average()
            .orElse(0.0);
    }
    
    /**
     * 服务销毁时关闭线程池
     */
    public void shutdown() {
        if (logExecutor != null) {
            logExecutor.shutdown();
            try {
                if (!logExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    logExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}