package org.duqiu.fly.autoapi.gateway;

import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试服务工厂
 * 用于创建各种类型的测试API服务
 */
public class TestServiceFactory {

    public static ApiService createUserQueryService(Long dataSourceId, Long creatorId) {
        ApiService service = new ApiService();
        service.setName("用户查询服务");
        service.setDescription("根据用户ID查询用户信息");
        service.setPath("/api/users/{userId}");
        service.setMethod(ApiService.HttpMethod.GET);
        service.setDataSourceId(dataSourceId);
        service.setSqlContent("SELECT * FROM users WHERE id = ${userId}");
        service.setRequestParams(createRequestParams(
            Map.of("userId", createParamConfig("number", true, "用户ID", "path"))
        ));
        service.setStatus(ApiStatus.PUBLISHED);
        service.setEnabled(true);
        service.setCreatedBy(creatorId);
        return service;
    }

    public static ApiService createMultiParamService(Long dataSourceId, Long creatorId) {
        ApiService service = new ApiService();
        service.setName("多参数查询服务");
        service.setDescription("根据多个条件查询用户");
        service.setPath("/api/users/search");
        service.setMethod(ApiService.HttpMethod.POST);
        service.setDataSourceId(dataSourceId);
        service.setSqlContent("SELECT * FROM users WHERE name LIKE ${name} AND age > ${minAge}");
        service.setRequestParams(createRequestParams(
            Map.of(
                "name", createParamConfig("string", false, "姓名模糊匹配", "query"),
                "minAge", createParamConfig("number", false, "最小年龄", "query")
            )
        ));
        service.setStatus(ApiStatus.PUBLISHED);
        service.setEnabled(true);
        service.setCreatedBy(creatorId);
        return service;
    }

    public static ApiService createProductService(Long dataSourceId, Long creatorId) {
        ApiService service = new ApiService();
        service.setName("产品查询服务");
        service.setDescription("根据分类和价格查询产品");
        service.setPath("/api/products");
        service.setMethod(ApiService.HttpMethod.GET);
        service.setDataSourceId(dataSourceId);
        service.setSqlContent("SELECT * FROM products WHERE category = ${category} AND price <= ${maxPrice}");
        service.setRequestParams(createRequestParams(
            Map.of(
                "category", createParamConfig("string", true, "产品分类", "query"),
                "maxPrice", createParamConfig("number", false, "最大价格", "query")
            )
        ));
        service.setStatus(ApiStatus.PUBLISHED);
        service.setEnabled(true);
        service.setCreatedBy(creatorId);
        return service;
    }

    public static ApiService createUserCreationService(Long dataSourceId, Long creatorId) {
        ApiService service = new ApiService();
        service.setName("用户创建服务");
        service.setDescription("创建新用户");
        service.setPath("/api/users");
        service.setMethod(ApiService.HttpMethod.POST);
        service.setDataSourceId(dataSourceId);
        service.setSqlContent("INSERT INTO users (name, email, age) VALUES (${name}, ${email}, ${age})");
        service.setRequestParams(createRequestParams(
            Map.of(
                "name", createParamConfig("string", true, "用户姓名", "body"),
                "email", createParamConfig("string", true, "邮箱地址", "body"),
                "age", createParamConfig("number", false, "用户年龄", "body")
            )
        ));
        service.setStatus(ApiStatus.PUBLISHED);
        service.setEnabled(true);
        service.setCreatedBy(creatorId);
        return service;
    }

    public static ApiService createMalformedSqlService(Long dataSourceId, Long creatorId) {
        ApiService service = new ApiService();
        service.setName("错误SQL服务");
        service.setDescription("包含语法错误的SQL");
        service.setPath("/api/malformed");
        service.setMethod(ApiService.HttpMethod.GET);
        service.setDataSourceId(dataSourceId);
        service.setSqlContent("SELECT FROM WHERE"); // 错误的SQL
        service.setRequestParams("{}");
        service.setStatus(ApiStatus.PUBLISHED);
        service.setEnabled(true);
        service.setCreatedBy(creatorId);
        return service;
    }

    public static ApiService createDraftService(Long dataSourceId, Long creatorId) {
        ApiService service = new ApiService();
        service.setName("草稿服务");
        service.setDescription("未发布的草稿服务");
        service.setPath("/api/draft");
        service.setMethod(ApiService.HttpMethod.GET);
        service.setDataSourceId(dataSourceId);
        service.setSqlContent("SELECT 1");
        service.setRequestParams("{}");
        service.setStatus(ApiStatus.DRAFT);
        service.setEnabled(true);
        service.setCreatedBy(creatorId);
        return service;
    }

    private static Map<String, Object> createParamConfig(String type, boolean required, String description, String in) {
        Map<String, Object> config = new HashMap<>();
        config.put("type", type);
        config.put("required", required);
        config.put("description", description);
        config.put("in", in);
        return config;
    }

    private static String createRequestParams(Map<String, Map<String, Object>> params) {
        // 简化实现，实际项目中可以使用JSON库
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Map<String, Object>> entry : params.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("\"").append(entry.getKey()).append("\": {");
            
            boolean firstParam = true;
            for (Map.Entry<String, Object> paramEntry : entry.getValue().entrySet()) {
                if (!firstParam) {
                    sb.append(", ");
                }
                sb.append("\"").append(paramEntry.getKey()).append("\": ");
                if (paramEntry.getValue() instanceof String) {
                    sb.append("\"").append(paramEntry.getValue()).append("\"");
                } else if (paramEntry.getValue() instanceof Boolean) {
                    sb.append(paramEntry.getValue());
                } else {
                    sb.append("\"").append(paramEntry.getValue()).append("\"");
                }
                firstParam = false;
            }
            sb.append("}");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    public static Map<String, Object> createTestParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 1);
        params.put("name", "%John%");
        params.put("minAge", 18);
        params.put("category", "electronics");
        params.put("maxPrice", 1000);
        params.put("email", "test@example.com");
        return params;
    }
}