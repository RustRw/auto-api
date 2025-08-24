package org.duqiu.fly.autoapi.test.dto;

import lombok.Data;
import org.duqiu.fly.autoapi.api.model.ApiService;

import java.util.Map;

@Data
public class ApiTestRequest {
    private Long apiServiceId;
    private Map<String, Object> parameters;
    private ApiService.HttpMethod method;
    private String path;
}