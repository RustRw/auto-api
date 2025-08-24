package org.duqiu.fly.autoapi.gateway.service;

import org.duqiu.fly.autoapi.datasource.core.ConnectionPool;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据源连接池服务
 * 负责管理数据源连接池的生命周期和自动清理
 */
@Service
public class DataSourceConnectionPoolService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConnectionPoolService.class);
    
    @Autowired
    private DataSourceRepository dataSourceRepository;
    
    @Autowired
    private UnifiedDataSourceFactory dataSourceFactory;
    
    /**
     * 活跃的数据源连接池映射
     * Key: 数据源ID
     * Value: 连接池实例
     */
    private final Map<Long, ConnectionPool> activeConnectionPools = new ConcurrentHashMap<>();
    
    /**
     * 数据源使用计数器
     * Key: 数据源ID  
     * Value: 使用次数
     */
    private final Map<Long, AtomicInteger> dataSourceUsageCount = new ConcurrentHashMap<>();
    
    /**
     * 初始化服务
     */
    @PostConstruct
    public void init() {
        logger.info("Initializing DataSource Connection Pool Service...");
    }
    
    /**
     * 确保数据源连接池可用
     * @param dataSourceId 数据源ID
     */
    public void ensureDataSourceConnection(Long dataSourceId) {
        if (dataSourceId == null) {
            return;
        }
        
        try {
            // 增加使用计数
            dataSourceUsageCount
                .computeIfAbsent(dataSourceId, id -> new AtomicInteger(0))
                .incrementAndGet();
            
            // 如果连接池不存在，则创建
            if (!activeConnectionPools.containsKey(dataSourceId)) {
                createConnectionPool(dataSourceId);
            }
            
            logger.debug("Ensured connection pool for datasource: {}", dataSourceId);
            
        } catch (Exception e) {
            logger.error("Failed to ensure connection pool for datasource: {}", dataSourceId, e);
        }
    }
    
    /**
     * 创建数据源连接池
     * @param dataSourceId 数据源ID
     */
    private synchronized void createConnectionPool(Long dataSourceId) {
        if (activeConnectionPools.containsKey(dataSourceId)) {
            return;
        }
        
        try {
            DataSource dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + dataSourceId));
            
            ConnectionPool connectionPool = dataSourceFactory.getConnectionPool(dataSource);
            activeConnectionPools.put(dataSourceId, connectionPool);
            
            logger.info("Created connection pool for datasource: {} ({})", 
                       dataSourceId, dataSource.getName());
            
        } catch (Exception e) {
            logger.error("Failed to create connection pool for datasource: {}", dataSourceId, e);
        }
    }
    
    /**
     * 检查并清理无用的数据源连接池
     * @param dataSourceId 数据源ID
     */
    public void checkAndCleanupDataSourceConnection(Long dataSourceId) {
        if (dataSourceId == null) {
            return;
        }
        
        try {
            // 检查是否有服务在使用此数据源
            boolean isInUse = isDataSourceInUse(dataSourceId);
            
            if (!isInUse) {
                // 如果没有服务使用，清理连接池
                cleanupConnectionPool(dataSourceId);
                logger.info("Cleaned up connection pool for unused datasource: {}", dataSourceId);
            } else {
                logger.debug("Datasource {} is still in use, skipping cleanup", dataSourceId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to check and cleanup datasource connection: {}", dataSourceId, e);
        }
    }
    
    /**
     * 检查数据源是否正在被使用
     * @param dataSourceId 数据源ID
     * @return 是否正在使用
     */
    private boolean isDataSourceInUse(Long dataSourceId) {
        // 这里需要检查是否有API服务正在使用此数据源
        // 在实际实现中，应该查询API服务表来确认
        
        // 临时实现：检查使用计数
        AtomicInteger usageCount = dataSourceUsageCount.get(dataSourceId);
        return usageCount != null && usageCount.get() > 0;
    }
    
    /**
     * 清理数据源连接池
     * @param dataSourceId 数据源ID
     */
    private synchronized void cleanupConnectionPool(Long dataSourceId) {
        ConnectionPool connectionPool = activeConnectionPools.remove(dataSourceId);
        dataSourceUsageCount.remove(dataSourceId);
        
        if (connectionPool != null) {
            try {
                connectionPool.close();
                logger.info("Closed connection pool for datasource: {}", dataSourceId);
            } catch (Exception e) {
                logger.warn("Failed to close connection pool for datasource: {}", dataSourceId, e);
            }
        }
    }
    
    /**
     * 定时清理长时间未使用的连接池
     * 每小时执行一次
     */
    @Scheduled(fixedDelay = 3600000) // 1小时
    public void scheduledCleanup() {
        logger.debug("Starting scheduled connection pool cleanup...");
        
        try {
            // 获取所有活跃的数据源ID
            activeConnectionPools.keySet().forEach(dataSourceId -> {
                if (!isDataSourceInUse(dataSourceId)) {
                    cleanupConnectionPool(dataSourceId);
                    logger.info("Scheduled cleanup: removed unused connection pool for datasource: {}", dataSourceId);
                }
            });
            
            logger.debug("Scheduled connection pool cleanup completed");
            
        } catch (Exception e) {
            logger.error("Error during scheduled connection pool cleanup", e);
        }
    }
    
    /**
     * 获取连接池
     * @param dataSourceId 数据源ID
     * @return 连接池实例
     */
    public ConnectionPool getConnectionPool(Long dataSourceId) {
        return activeConnectionPools.get(dataSourceId);
    }
    
    /**
     * 获取活跃连接池数量
     * @return 连接池数量
     */
    public int getActivePoolCount() {
        return activeConnectionPools.size();
    }
    
    /**
     * 获取数据源使用统计
     * @return 使用统计映射
     */
    public Map<Long, Integer> getDataSourceUsageStatistics() {
        Map<Long, Integer> statistics = new ConcurrentHashMap<>();
        dataSourceUsageCount.forEach((id, count) -> statistics.put(id, count.get()));
        return statistics;
    }
    
    /**
     * 服务销毁时清理所有连接池
     */
    @PreDestroy
    public void cleanupAll() {
        logger.info("Cleaning up all connection pools...");
        
        activeConnectionPools.forEach((dataSourceId, connectionPool) -> {
            try {
                connectionPool.close();
                logger.info("Closed connection pool for datasource: {}", dataSourceId);
            } catch (Exception e) {
                logger.warn("Failed to close connection pool for datasource: {}", dataSourceId, e);
            }
        });
        
        activeConnectionPools.clear();
        dataSourceUsageCount.clear();
        
        logger.info("All connection pools cleaned up");
    }
}