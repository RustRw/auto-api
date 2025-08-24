package org.duqiu.fly.autoapi.gateway.service;

import org.duqiu.fly.autoapi.api.service.ApiServiceExecutor;
import org.duqiu.fly.autoapi.api.dto.ApiServiceResponse;
import org.duqiu.fly.autoapi.api.dto.ApiServiceVersionResponse;
import org.duqiu.fly.autoapi.datasource.service.DataSourceService;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponse;
import org.duqiu.fly.autoapi.gateway.dto.ApiServiceInfo;
import org.duqiu.fly.autoapi.gateway.dto.ServiceRequestLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * API服务发现服务
 * 负责定时发现、更新、管理已发布的API服务
 */
@Service
public class ApiServiceDiscoveryService {
    
    private static final Logger logger = LoggerFactory.getLogger(ApiServiceDiscoveryService.class);
    
    @Autowired
    private ApiServiceExecutor apiServiceExecutor;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private DataSourceConnectionPoolService connectionPoolService;
    
    @Autowired
    private ServiceRequestLogService requestLogService;
    
    /**
     * 当前活跃的API服务缓存
     * Key: 服务唯一标识(method:path:version)
     * Value: API服务信息
     */
    private final Map<String, ApiServiceInfo> activeServices = new ConcurrentHashMap<>();
    
    /**
     * 上次扫描的时间戳
     */
    private volatile LocalDateTime lastScanTime;
    
    /**
     * 服务变更统计
     */
    private final Map<String, Integer> changeStatistics = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        logger.info("Initializing API Service Discovery Service...");
        changeStatistics.put("added", 0);
        changeStatistics.put("updated", 0);
        changeStatistics.put("removed", 0);
        
        // 初始化时执行一次服务发现
        discoverAndUpdateServices();
        logger.info("API Service Discovery Service initialized with {} active services", 
                    activeServices.size());
    }
    
    /**
     * 定时服务发现和更新
     * 每30秒执行一次
     */
    @Scheduled(fixedDelay = 30000)
    public void scheduledServiceDiscovery() {
        try {
            discoverAndUpdateServices();
        } catch (Exception e) {
            logger.error("Error during scheduled service discovery", e);
        }
    }
    
    /**
     * 执行服务发现和更新
     */
    public synchronized void discoverAndUpdateServices() {
        logger.debug("Starting service discovery and update process...");
        LocalDateTime scanStartTime = LocalDateTime.now();
        
        // 记录发现开始事件
        Map<String, Object> discoveryStartDetails = new HashMap<>();
        discoveryStartDetails.put("scanStartTime", scanStartTime);
        discoveryStartDetails.put("currentActiveServices", activeServices.size());
        logServiceDiscoveryEvent("DISCOVERY_START", "Service discovery process started", discoveryStartDetails);
        
        try {
            // 获取所有已发布的API服务
            List<ApiServiceResponse> publishedServices = getPublishedApiServices();
            logger.debug("Found {} published API services", publishedServices.size());
            
            // 构建新的服务映射
            Map<String, ApiServiceInfo> newServiceMap = new HashMap<>();
            
            for (ApiServiceResponse service : publishedServices) {
                try {
                    // 获取活跃版本
                    ApiServiceVersionResponse activeVersion = getActiveVersion(service.getId());
                    if (activeVersion != null) {
                        ApiServiceInfo serviceInfo = createServiceInfo(service, activeVersion);
                        newServiceMap.put(serviceInfo.getServiceKey(), serviceInfo);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to process service {}: {}", service.getId(), e.getMessage());
                    
                    // 记录处理失败日志
                    Map<String, Object> errorDetails = new HashMap<>();
                    errorDetails.put("serviceId", service.getId());
                    errorDetails.put("serviceName", service.getName());
                    errorDetails.put("error", e.getMessage());
                    logServiceDiscoveryEvent("PROCESS_ERROR", 
                                           "Failed to process service: " + service.getId(), 
                                           errorDetails);
                }
            }
            
            // 检测服务变更
            detectServiceChanges(newServiceMap);
            
            // 更新服务缓存
            activeServices.clear();
            activeServices.putAll(newServiceMap);
            
            lastScanTime = scanStartTime;
            
            logger.info("Service discovery completed. Active services: {}, Added: {}, Updated: {}, Removed: {}",
                       activeServices.size(),
                       changeStatistics.get("added"),
                       changeStatistics.get("updated"), 
                       changeStatistics.get("removed"));
            
            // 记录发现完成事件
            Map<String, Object> discoveryCompleteDetails = new HashMap<>();
            discoveryCompleteDetails.put("scanStartTime", scanStartTime);
            discoveryCompleteDetails.put("scanEndTime", LocalDateTime.now());
            discoveryCompleteDetails.put("publishedServicesCount", publishedServices.size());
            discoveryCompleteDetails.put("activeServicesCount", activeServices.size());
            discoveryCompleteDetails.put("servicesAdded", changeStatistics.get("added"));
            discoveryCompleteDetails.put("servicesUpdated", changeStatistics.get("updated"));
            discoveryCompleteDetails.put("servicesRemoved", changeStatistics.get("removed"));
            logServiceDiscoveryEvent("DISCOVERY_COMPLETE", 
                                   "Service discovery completed successfully", 
                                   discoveryCompleteDetails);
                       
        } catch (Exception e) {
            logger.error("Error during service discovery and update", e);
            
            // 记录发现失败事件
            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("error", e.getMessage());
            errorDetails.put("scanStartTime", scanStartTime);
            logServiceDiscoveryEvent("DISCOVERY_ERROR", 
                                   "Service discovery failed: " + e.getMessage(), 
                                   errorDetails);
        }
    }
    
    /**
     * 获取所有已发布的API服务
     */
    private List<ApiServiceResponse> getPublishedApiServices() {
        try {
            Page<ApiServiceResponse> servicesPage = apiServiceExecutor.getPublishedApiServices(
                PageRequest.of(0, 1000)
            );
            return servicesPage.getContent();
        } catch (Exception e) {
            logger.error("Failed to get published API services", e);
            return List.of();
        }
    }
    
    /**
     * 获取API服务的活跃版本
     */
    private ApiServiceVersionResponse getActiveVersion(Long serviceId) {
        try {
            Page<ApiServiceVersionResponse> versions = apiServiceExecutor.getVersions(
                serviceId, 1L, PageRequest.of(0, 10)
            );
            
            return versions.getContent().stream()
                    .filter(ApiServiceVersionResponse::getIsActive)
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            logger.warn("Failed to get active version for service {}: {}", serviceId, e.getMessage());
            return null;
        }
    }
    
    /**
     * 创建API服务信息对象
     */
    private ApiServiceInfo createServiceInfo(ApiServiceResponse service, ApiServiceVersionResponse version) {
        ApiServiceInfo serviceInfo = new ApiServiceInfo(
            service.getId(),
            service.getName(),
            service.getPath(),
            service.getMethod().name(),
            version.getVersion(),
            version.getIsActive(),
            version.getSqlContent(),
            service.getDataSourceId()
        );
        
        serviceInfo.setDescription(service.getDescription());
        serviceInfo.setCreatedBy(String.valueOf(service.getCreatedBy()));
        serviceInfo.setCreatedAt(service.getCreatedAt());
        serviceInfo.setUpdatedAt(service.getUpdatedAt());
        
        // 获取数据源配置信息
        try {
            DataSourceResponse dataSource = dataSourceService.getDataSourceById(service.getDataSourceId(), 1L);
            Map<String, Object> dataSourceConfig = new HashMap<>();
            dataSourceConfig.put("name", dataSource.getName());
            dataSourceConfig.put("type", dataSource.getType());
            dataSourceConfig.put("host", dataSource.getHost());
            dataSourceConfig.put("port", dataSource.getPort());
            dataSourceConfig.put("database", dataSource.getDatabase());
            serviceInfo.setDataSourceConfig(dataSourceConfig);
        } catch (Exception e) {
            logger.warn("Failed to get datasource config for service {}: {}", 
                       service.getId(), e.getMessage());
        }
        
        return serviceInfo;
    }
    
    /**
     * 检测服务变更
     */
    private void detectServiceChanges(Map<String, ApiServiceInfo> newServiceMap) {
        // 检测新增和更新的服务
        for (Map.Entry<String, ApiServiceInfo> entry : newServiceMap.entrySet()) {
            String serviceKey = entry.getKey();
            ApiServiceInfo newService = entry.getValue();
            ApiServiceInfo existingService = activeServices.get(serviceKey);
            
            if (existingService == null) {
                // 新增服务
                handleServiceAdded(newService);
                changeStatistics.merge("added", 1, Integer::sum);
            } else if (isServiceUpdated(existingService, newService)) {
                // 更新服务
                handleServiceUpdated(existingService, newService);
                changeStatistics.merge("updated", 1, Integer::sum);
            }
        }
        
        // 检测删除的服务
        for (Map.Entry<String, ApiServiceInfo> entry : activeServices.entrySet()) {
            String serviceKey = entry.getKey();
            if (!newServiceMap.containsKey(serviceKey)) {
                // 删除服务
                handleServiceRemoved(entry.getValue());
                changeStatistics.merge("removed", 1, Integer::sum);
            }
        }
    }
    
    /**
     * 判断服务是否已更新
     */
    private boolean isServiceUpdated(ApiServiceInfo existing, ApiServiceInfo newService) {
        return !existing.getUpdatedAt().equals(newService.getUpdatedAt()) ||
               !existing.getSqlContent().equals(newService.getSqlContent()) ||
               !existing.getIsActive().equals(newService.getIsActive());
    }
    
    /**
     * 处理服务新增
     */
    private void handleServiceAdded(ApiServiceInfo service) {
        logger.info("New API service detected: {} [{}:{}:{}]", 
                   service.getServiceName(),
                   service.getHttpMethod(),
                   service.getServicePath(),
                   service.getVersion());
        
        // 记录服务新增日志
        logServiceChange("added", service, "New service discovered");
        
        // 确保数据源连接池可用
        connectionPoolService.ensureDataSourceConnection(service.getDataSourceId());
    }
    
    /**
     * 处理服务更新
     */
    private void handleServiceUpdated(ApiServiceInfo oldService, ApiServiceInfo newService) {
        logger.info("API service updated: {} [{}:{}:{}]", 
                   newService.getServiceName(),
                   newService.getHttpMethod(),
                   newService.getServicePath(),
                   newService.getVersion());
        
        // 记录服务更新日志
        logServiceChange("updated", newService, "Service configuration updated");
        
        // 确保数据源连接池可用
        connectionPoolService.ensureDataSourceConnection(newService.getDataSourceId());
    }
    
    /**
     * 处理服务删除
     */
    private void handleServiceRemoved(ApiServiceInfo service) {
        logger.info("API service removed: {} [{}:{}:{}]", 
                   service.getServiceName(),
                   service.getHttpMethod(),
                   service.getServicePath(),
                   service.getVersion());
        
        // 记录服务删除日志
        logServiceChange("removed", service, "Service no longer available");
        
        // 检查是否需要清理数据源连接池
        connectionPoolService.checkAndCleanupDataSourceConnection(service.getDataSourceId());
    }
    
    /**
     * 获取当前活跃服务列表
     */
    public List<ApiServiceInfo> getActiveServices() {
        return activeServices.values().stream().collect(Collectors.toList());
    }
    
    /**
     * 根据服务键获取服务信息
     */
    public ApiServiceInfo getService(String serviceKey) {
        return activeServices.get(serviceKey);
    }
    
    /**
     * 根据路径和方法获取服务信息
     */
    public ApiServiceInfo getService(String method, String path) {
        return activeServices.values().stream()
                .filter(service -> service.getHttpMethod().equalsIgnoreCase(method) && 
                                 service.getServicePath().equals(path))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取服务变更统计
     */
    public Map<String, Integer> getChangeStatistics() {
        return new HashMap<>(changeStatistics);
    }
    
    /**
     * 重置变更统计
     */
    public void resetChangeStatistics() {
        changeStatistics.replaceAll((k, v) -> 0);
    }
    
    /**
     * 获取上次扫描时间
     */
    public LocalDateTime getLastScanTime() {
        return lastScanTime;
    }
    
    /**
     * 获取服务总数
     */
    public int getActiveServiceCount() {
        return activeServices.size();
    }
    
    /**
     * 记录服务发现事件日志
     */
    private void logServiceDiscoveryEvent(String eventType, String message, Map<String, Object> details) {
        ServiceRequestLog log = requestLogService.createRequestLog(
            "SERVICE_DISCOVERY", 
            null, 
            "internal://service-discovery"
        );
        
        log.setHttpMethod("SCHEDULED");
        log.setServiceVersion("1.0");
        log.setRequestParameters(details);
        log.setExecutedSqlScript("N/A");
        log.setIsSuccess(true);
        log.setRecordCount(activeServices.size());
        log.setResponseSizeBytes(0L);
        log.setClientIpAddress("127.0.0.1");
        log.setUserAgent("ApiServiceDiscoveryService");
        log.setUsername("system");
        log.setUserId("system");
        log.setErrorMessage(eventType + ": " + message);
        
        log.markRequestCompleted(true);
        requestLogService.logServiceRequest(log);
    }
    
    /**
     * 记录服务变更详细日志
     */
    private void logServiceChange(String changeType, ApiServiceInfo serviceInfo, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("serviceId", serviceInfo.getServiceId());
        details.put("serviceName", serviceInfo.getServiceName());
        details.put("servicePath", serviceInfo.getServicePath());
        details.put("httpMethod", serviceInfo.getHttpMethod());
        details.put("version", serviceInfo.getVersion());
        details.put("dataSourceId", serviceInfo.getDataSourceId());
        details.put("reason", reason);
        
        logServiceDiscoveryEvent("SERVICE_" + changeType.toUpperCase(), 
                               "Service " + changeType + ": " + serviceInfo.getServiceName(), 
                               details);
    }
}