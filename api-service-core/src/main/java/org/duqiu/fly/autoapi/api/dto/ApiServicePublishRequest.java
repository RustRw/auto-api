package org.duqiu.fly.autoapi.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * API服务发布请求DTO
 */
@Data
public class ApiServicePublishRequest {
    
    /**
     * 版本号
     */
    @NotBlank(message = "版本号不能为空")
    private String version;
    
    /**
     * 版本描述
     */
    private String versionDescription;
    
    /**
     * 是否强制发布（覆盖已存在的版本）
     */
    private Boolean forcePublish = false;
}