package org.duqiu.fly.autoapi.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.duqiu.fly.autoapi.api.dto.*;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.model.ApiServiceAuditLog;
import org.duqiu.fly.autoapi.api.model.ApiServiceVersion;
import org.duqiu.fly.autoapi.api.repository.ApiServiceAuditLogRepository;
import org.duqiu.fly.autoapi.api.repository.ApiServiceRepository;
import org.duqiu.fly.autoapi.api.repository.ApiServiceVersionRepository;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 增强的API服务管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiServiceExecutor {
    
    private final ApiServiceRepository apiServiceRepository;
    private final ApiServiceVersionRepository versionRepository;
    private final ApiServiceAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    /**
     * 创建API服务
     */
    @Transactional
    public ApiServiceResponse createApiService(ApiServiceCreateRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 检查名称唯一性
            if (apiServiceRepository.existsByNameAndCreatedBy(request.getName(), userId)) {
                throw new IllegalArgumentException("API服务名称已存在");
            }
            
            // 创建API服务实体
            ApiService apiService = new ApiService();
            apiService.setName(request.getName());
            apiService.setDescription(request.getDescription());
            apiService.setPath(request.getPath());
            apiService.setMethod(request.getMethod());
            apiService.setDataSourceId(request.getDataSourceId());
            apiService.setSqlContent(request.getSqlContent());
            apiService.setRequestParams(request.getRequestParams());
            apiService.setCacheEnabled(request.getCacheEnabled());
            apiService.setCacheDuration(request.getCacheDuration());
            apiService.setRateLimit(request.getRateLimit());
            apiService.setStatus(ApiStatus.DRAFT);
            apiService.setCreatedBy(userId);
            apiService.setUpdatedBy(userId);
            
            // 保存
            apiService = apiServiceRepository.save(apiService);
            
            // 记录操作日志
            recordAuditLog(apiService.getId(), ApiServiceAuditLog.OperationType.CREATE, 
                    "创建API服务", null, apiService, userId, 
                    ApiServiceAuditLog.OperationResult.SUCCESS, 
                    System.currentTimeMillis() - startTime);
            
            return convertToResponse(apiService);
            
        } catch (Exception e) {
            log.error("创建API服务失败", e);
            // 记录失败日志
            recordAuditLog(null, ApiServiceAuditLog.OperationType.CREATE, 
                    "创建API服务失败", null, null, userId, 
                    ApiServiceAuditLog.OperationResult.FAILED, 
                    System.currentTimeMillis() - startTime, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 更新API服务
     */
    @Transactional
    public ApiServiceResponse updateApiService(Long id, ApiServiceUpdateRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            ApiService apiService = getApiServiceByIdAndUser(id, userId);
            ApiService beforeData = cloneApiService(apiService);
            
            // 只允许更新草稿状态的服务
            if (apiService.getStatus() != ApiStatus.DRAFT) {
                throw new IllegalStateException("只能修改草稿状态的API服务");
            }
            
            // 更新字段
            apiService.setName(request.getName());
            apiService.setDescription(request.getDescription());
            apiService.setPath(request.getPath());
            apiService.setMethod(request.getMethod());
            apiService.setSqlContent(request.getSqlContent());
            apiService.setRequestParams(request.getRequestParams());
            apiService.setResponseExample(request.getResponseExample());
            apiService.setCacheEnabled(request.getCacheEnabled());
            apiService.setCacheDuration(request.getCacheDuration());
            apiService.setRateLimit(request.getRateLimit());
            apiService.setUpdatedBy(userId);
            
            apiService = apiServiceRepository.save(apiService);
            
            // 记录操作日志
            recordAuditLog(id, ApiServiceAuditLog.OperationType.UPDATE, 
                    "更新API服务: " + (request.getUpdateDescription() != null ? request.getUpdateDescription() : ""), 
                    beforeData, apiService, userId, 
                    ApiServiceAuditLog.OperationResult.SUCCESS, 
                    System.currentTimeMillis() - startTime);
            
            return convertToResponse(apiService);
            
        } catch (Exception e) {
            log.error("更新API服务失败", e);
            recordAuditLog(id, ApiServiceAuditLog.OperationType.UPDATE, 
                    "更新API服务失败", null, null, userId, 
                    ApiServiceAuditLog.OperationResult.FAILED, 
                    System.currentTimeMillis() - startTime, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 发布API服务
     */
    @Transactional
    public ApiServiceVersionResponse publishApiService(Long id, ApiServicePublishRequest request, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            ApiService apiService = getApiServiceByIdAndUser(id, userId);
            
            // 检查是否可以发布
            if (apiService.getStatus() == ApiStatus.DISABLED) {
                throw new IllegalStateException("已禁用的服务无法发布");
            }
            
            // 检查版本号是否已存在
            Optional<ApiServiceVersion> existingVersion = 
                    versionRepository.findByApiServiceIdAndVersion(id, request.getVersion());
            if (existingVersion.isPresent() && !request.getForcePublish()) {
                throw new IllegalArgumentException("版本号已存在，请使用不同的版本号或选择强制发布");
            }
            
            // 创建版本快照
            ApiServiceVersion version = createVersionSnapshot(apiService, request);
            version = versionRepository.save(version);
            
            // 设置为激活版本
            versionRepository.setActiveVersion(id, version.getId());
            
            // 更新服务状态
            apiService.setStatus(ApiStatus.PUBLISHED);
            apiService.setUpdatedBy(userId);
            apiServiceRepository.save(apiService);
            
            // 记录操作日志
            recordAuditLog(id, ApiServiceAuditLog.OperationType.PUBLISH, 
                    "发布API服务版本: " + request.getVersion(), 
                    apiService, version, userId, 
                    ApiServiceAuditLog.OperationResult.SUCCESS, 
                    System.currentTimeMillis() - startTime);
            
            return convertToVersionResponse(version);
            
        } catch (Exception e) {
            log.error("发布API服务失败", e);
            recordAuditLog(id, ApiServiceAuditLog.OperationType.PUBLISH, 
                    "发布API服务失败", null, null, userId, 
                    ApiServiceAuditLog.OperationResult.FAILED, 
                    System.currentTimeMillis() - startTime, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 下线API服务
     */
    @Transactional
    public void unpublishApiService(Long id, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            ApiService apiService = getApiServiceByIdAndUser(id, userId);
            
            if (apiService.getStatus() != ApiStatus.PUBLISHED) {
                throw new IllegalStateException("只能下线已发布的服务");
            }
            
            // 更新服务状态
            ApiService beforeData = cloneApiService(apiService);
            apiService.setStatus(ApiStatus.DISABLED);
            apiService.setUpdatedBy(userId);
            apiServiceRepository.save(apiService);
            
            // 更新当前激活版本状态
            Optional<ApiServiceVersion> activeVersion = 
                    versionRepository.findActiveVersionByApiServiceId(id);
            if (activeVersion.isPresent()) {
                ApiServiceVersion version = activeVersion.get();
                version.setUnpublishedAt(LocalDateTime.now());
                version.setIsActive(false);
                versionRepository.save(version);
            }
            
            // 记录操作日志
            recordAuditLog(id, ApiServiceAuditLog.OperationType.UNPUBLISH, 
                    "下线API服务", beforeData, apiService, userId, 
                    ApiServiceAuditLog.OperationResult.SUCCESS, 
                    System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("下线API服务失败", e);
            recordAuditLog(id, ApiServiceAuditLog.OperationType.UNPUBLISH, 
                    "下线API服务失败", null, null, userId, 
                    ApiServiceAuditLog.OperationResult.FAILED, 
                    System.currentTimeMillis() - startTime, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 获取API服务版本列表
     */
    public Page<ApiServiceVersionResponse> getVersions(Long apiServiceId, Long userId, Pageable pageable) {
        // 验证权限
        getApiServiceByIdAndUser(apiServiceId, userId);
        
        Page<ApiServiceVersion> versions = 
                versionRepository.findByApiServiceIdOrderByCreatedAtDesc(apiServiceId, pageable);
        
        return versions.map(this::convertToVersionResponse);
    }
    
    /**
     * 版本对比
     */
    public ApiServiceVersionCompareResponse compareVersions(Long apiServiceId, String sourceVersion, 
                                                           String targetVersion, Long userId) {
        // 验证权限
        getApiServiceByIdAndUser(apiServiceId, userId);
        
        // 获取两个版本
        ApiServiceVersion source = versionRepository
                .findByApiServiceIdAndVersion(apiServiceId, sourceVersion)
                .orElseThrow(() -> new IllegalArgumentException("源版本不存在"));
                
        ApiServiceVersion target = versionRepository
                .findByApiServiceIdAndVersion(apiServiceId, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException("目标版本不存在"));
        
        // 进行对比
        ApiServiceVersionCompareResponse response = new ApiServiceVersionCompareResponse();
        response.setSourceVersion(convertToVersionResponse(source));
        response.setTargetVersion(convertToVersionResponse(target));
        response.setDifferences(compareVersionDetails(source, target));
        
        // 记录对比操作
        recordAuditLog(apiServiceId, ApiServiceAuditLog.OperationType.VERSION_COMPARE, 
                String.format("版本对比: %s vs %s", sourceVersion, targetVersion), 
                source, target, userId, ApiServiceAuditLog.OperationResult.SUCCESS, 0L);
        
        return response;
    }
    
    /**
     * 删除API服务
     */
    @Transactional
    public void deleteApiService(Long id, Long userId) {
        long startTime = System.currentTimeMillis();
        
        try {
            ApiService apiService = getApiServiceByIdAndUser(id, userId);
            
            // 只允许删除草稿状态的服务
            if (apiService.getStatus() != ApiStatus.DRAFT) {
                throw new IllegalStateException("只能删除草稿状态的API服务");
            }
            
            // 删除相关数据
            versionRepository.deleteByApiServiceId(id);
            auditLogRepository.deleteByApiServiceId(id);
            apiServiceRepository.delete(apiService);
            
            // 记录操作日志（在新事务中）
            recordAuditLog(id, ApiServiceAuditLog.OperationType.DELETE, 
                    "删除API服务", apiService, null, userId, 
                    ApiServiceAuditLog.OperationResult.SUCCESS, 
                    System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            log.error("删除API服务失败", e);
            recordAuditLog(id, ApiServiceAuditLog.OperationType.DELETE, 
                    "删除API服务失败", null, null, userId, 
                    ApiServiceAuditLog.OperationResult.FAILED, 
                    System.currentTimeMillis() - startTime, e.getMessage());
            throw e;
        }
    }
    
    /**
     * 获取操作日志
     */
    public Page<ApiServiceAuditLog> getAuditLogs(Long apiServiceId, Long userId, Pageable pageable) {
        // 验证权限
        getApiServiceByIdAndUser(apiServiceId, userId);
        
        return auditLogRepository.findByApiServiceIdOrderByCreatedAtDesc(apiServiceId, pageable);
    }

    /**
     * 获取已发布的API服务
     */
    public Page<ApiServiceResponse> getPublishedApiServices(Pageable pageable) {
        Page<ApiService> publishedServices = apiServiceRepository.findByStatus(ApiStatus.PUBLISHED, pageable);
        return publishedServices.map(this::convertToResponse);
    }
    
    // ===== 私有方法 =====
    
    private ApiService getApiServiceByIdAndUser(Long id, Long userId) {
        return apiServiceRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("API服务不存在或无权限访问"));
    }
    
    private ApiServiceVersion createVersionSnapshot(ApiService apiService, ApiServicePublishRequest request) {
        ApiServiceVersion version = new ApiServiceVersion();
        version.setApiServiceId(apiService.getId());
        version.setVersion(request.getVersion());
        version.setVersionDescription(request.getVersionDescription());
        version.setName(apiService.getName());
        version.setDescription(apiService.getDescription());
        version.setPath(apiService.getPath());
        version.setMethod(apiService.getMethod());
        version.setDataSourceId(apiService.getDataSourceId());
        version.setSqlContent(apiService.getSqlContent());
        version.setRequestParams(apiService.getRequestParams());
        version.setResponseExample(apiService.getResponseExample());
        version.setStatus(ApiStatus.PUBLISHED);
        version.setIsActive(true);
        version.setPublishedAt(LocalDateTime.now());
        version.setCacheEnabled(apiService.getCacheEnabled());
        version.setCacheDuration(apiService.getCacheDuration());
        version.setRateLimit(apiService.getRateLimit());
        version.setCreatedBy(apiService.getCreatedBy());
        version.setUpdatedBy(apiService.getUpdatedBy());
        
        return version;
    }
    
    private List<ApiServiceVersionCompareResponse.VersionDifference> compareVersionDetails(
            ApiServiceVersion source, ApiServiceVersion target) {
        List<ApiServiceVersionCompareResponse.VersionDifference> differences = new ArrayList<>();
        
        compareField(differences, "name", "服务名称", source.getName(), target.getName());
        compareField(differences, "description", "服务描述", source.getDescription(), target.getDescription());
        compareField(differences, "path", "API路径", source.getPath(), target.getPath());
        compareField(differences, "method", "HTTP方法", source.getMethod(), target.getMethod());
        compareField(differences, "sqlContent", "SQL内容", source.getSqlContent(), target.getSqlContent());
        compareField(differences, "requestParams", "请求参数", source.getRequestParams(), target.getRequestParams());
        compareField(differences, "responseExample", "响应示例", source.getResponseExample(), target.getResponseExample());
        compareField(differences, "cacheEnabled", "缓存启用", source.getCacheEnabled(), target.getCacheEnabled());
        compareField(differences, "cacheDuration", "缓存时长", source.getCacheDuration(), target.getCacheDuration());
        compareField(differences, "rateLimit", "限流配置", source.getRateLimit(), target.getRateLimit());
        
        return differences;
    }
    
    private void compareField(List<ApiServiceVersionCompareResponse.VersionDifference> differences, 
                            String fieldName, String displayName, Object sourceValue, Object targetValue) {
        ApiServiceVersionCompareResponse.VersionDifference diff = 
                new ApiServiceVersionCompareResponse.VersionDifference();
        diff.setFieldName(fieldName);
        diff.setFieldDisplayName(displayName);
        diff.setSourceValue(sourceValue);
        diff.setTargetValue(targetValue);
        
        if (sourceValue == null && targetValue == null) {
            diff.setDifferenceType(ApiServiceVersionCompareResponse.DifferenceType.UNCHANGED);
        } else if (sourceValue == null) {
            diff.setDifferenceType(ApiServiceVersionCompareResponse.DifferenceType.ADDED);
        } else if (targetValue == null) {
            diff.setDifferenceType(ApiServiceVersionCompareResponse.DifferenceType.REMOVED);
        } else if (!sourceValue.equals(targetValue)) {
            diff.setDifferenceType(ApiServiceVersionCompareResponse.DifferenceType.MODIFIED);
        } else {
            diff.setDifferenceType(ApiServiceVersionCompareResponse.DifferenceType.UNCHANGED);
        }
        
        differences.add(diff);
    }
    
    private void recordAuditLog(Long apiServiceId, ApiServiceAuditLog.OperationType operationType,
                              String description, Object beforeData, Object afterData, Long userId,
                              ApiServiceAuditLog.OperationResult result, Long duration) {
        recordAuditLog(apiServiceId, operationType, description, beforeData, afterData, 
                      userId, result, duration, null);
    }
    
    private void recordAuditLog(Long apiServiceId, ApiServiceAuditLog.OperationType operationType,
                              String description, Object beforeData, Object afterData, Long userId,
                              ApiServiceAuditLog.OperationResult result, Long duration, String errorMessage) {
        try {
            ApiServiceAuditLog log = new ApiServiceAuditLog();
            log.setApiServiceId(apiServiceId);
            log.setOperationType(operationType);
            log.setOperationDescription(description);
            log.setOperationResult(result);
            log.setDurationMs(duration);
            log.setErrorMessage(errorMessage);
            log.setCreatedBy(userId);
            log.setUpdatedBy(userId);
            
            if (beforeData != null) {
                log.setBeforeData(objectMapper.writeValueAsString(beforeData));
            }
            if (afterData != null) {
                log.setAfterData(objectMapper.writeValueAsString(afterData));
            }
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.warn("记录审计日志失败", e);
        }
    }
    
    private ApiService cloneApiService(ApiService source) {
        try {
            String json = objectMapper.writeValueAsString(source);
            return objectMapper.readValue(json, ApiService.class);
        } catch (Exception e) {
            log.warn("克隆API服务对象失败", e);
            return null;
        }
    }
    
    private ApiServiceResponse convertToResponse(ApiService apiService) {
        ApiServiceResponse response = new ApiServiceResponse();
        response.setId(apiService.getId());
        response.setName(apiService.getName());
        response.setDescription(apiService.getDescription());
        response.setPath(apiService.getPath());
        response.setMethod(apiService.getMethod());
        response.setDataSourceId(apiService.getDataSourceId());
        response.setSqlContent(apiService.getSqlContent());
        response.setRequestParams(apiService.getRequestParams());
        response.setResponseExample(apiService.getResponseExample());
        response.setStatus(apiService.getStatus());
        response.setEnabled(apiService.getEnabled());
        response.setCacheEnabled(apiService.getCacheEnabled());
        response.setCacheDuration(apiService.getCacheDuration());
        response.setRateLimit(apiService.getRateLimit());
        response.setCreatedAt(apiService.getCreatedAt());
        response.setUpdatedAt(apiService.getUpdatedAt());
        response.setCreatedBy(apiService.getCreatedBy());
        response.setUpdatedBy(apiService.getUpdatedBy());
        
        return response;
    }
    
    private ApiServiceVersionResponse convertToVersionResponse(ApiServiceVersion version) {
        ApiServiceVersionResponse response = new ApiServiceVersionResponse();
        response.setId(version.getId());
        response.setApiServiceId(version.getApiServiceId());
        response.setVersion(version.getVersion());
        response.setVersionDescription(version.getVersionDescription());
        response.setName(version.getName());
        response.setDescription(version.getDescription());
        response.setPath(version.getPath());
        response.setMethod(version.getMethod());
        response.setDataSourceId(version.getDataSourceId());
        response.setSqlContent(version.getSqlContent());
        response.setRequestParams(version.getRequestParams());
        response.setResponseExample(version.getResponseExample());
        response.setStatus(version.getStatus());
        response.setIsActive(version.getIsActive());
        response.setPublishedAt(version.getPublishedAt());
        response.setUnpublishedAt(version.getUnpublishedAt());
        response.setCacheEnabled(version.getCacheEnabled());
        response.setCacheDuration(version.getCacheDuration());
        response.setRateLimit(version.getRateLimit());
        response.setCreatedAt(version.getCreatedAt());
        response.setCreatedBy(version.getCreatedBy());
        
        return response;
    }
}