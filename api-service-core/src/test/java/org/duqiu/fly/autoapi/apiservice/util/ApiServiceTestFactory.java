package org.duqiu.fly.autoapi.service.util;

import org.duqiu.fly.autoapi.api.dto.*;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.duqiu.fly.autoapi.datasource.model.DataSource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API服务测试工厂类 - 创建各种测试数据
 */
public class ApiServiceTestFactory {
    
    /**
     * 创建基本的API服务创建请求
     */
    public static ApiServiceCreateRequest createBasicApiServiceRequest() {
        ApiServiceCreateRequest request = new ApiServiceCreateRequest();
        request.setName("用户查询服务");
        request.setDescription("根据状态查询用户列表");
        request.setPath("/api/users");
        request.setMethod(ApiService.HttpMethod.GET);
        request.setDataSourceId(1L);
        request.setSqlContent("SELECT * FROM test_users WHERE status = ${status}");
        request.setRequestParams(createRequestParamsJson());
        request.setCacheEnabled(true);
        request.setCacheDuration(300);
        request.setRateLimit(1000);
        return request;
    }
    
    /**
     * 创建复杂的API服务创建请求
     */
    public static ApiServiceCreateRequest createComplexApiServiceRequest() {
        ApiServiceCreateRequest request = new ApiServiceCreateRequest();
        request.setName("订单详情查询服务");
        request.setDescription("查询用户订单详情，包括产品信息");
        request.setPath("/api/orders/details");
        request.setMethod(ApiService.HttpMethod.POST);
        request.setDataSourceId(1L);
        request.setSqlContent(
            "SELECT u.username, p.name as product_name, o.quantity, o.total_price, o.status " +
            "FROM test_orders o " +
            "JOIN test_users u ON o.user_id = u.id " +
            "JOIN test_products p ON o.product_id = p.id " +
            "WHERE o.status = ${orderStatus} " +
            "AND u.status = ${userStatus} " +
            "ORDER BY o.order_date DESC " +
            "LIMIT ${limit}"
        );
        request.setRequestParams(createComplexRequestParamsJson());
        request.setResponseExample(createResponseExampleJson());
        request.setCacheEnabled(false);
        request.setRateLimit(500);
        return request;
    }
    
    /**
     * 创建API服务更新请求
     */
    public static ApiServiceUpdateRequest createUpdateRequest() {
        ApiServiceUpdateRequest request = new ApiServiceUpdateRequest();
        request.setName("用户详情查询服务");
        request.setDescription("更新后的用户查询服务，增加了邮箱字段");
        request.setPath("/api/users/details");
        request.setMethod(ApiService.HttpMethod.GET);
        request.setSqlContent("SELECT id, username, email, status FROM test_users WHERE status = ${status}");
        request.setUpdateDescription("增加了email字段返回");
        return request;
    }
    
    /**
     * 创建API服务发布请求
     */
    public static ApiServicePublishRequest createPublishRequest(String version) {
        ApiServicePublishRequest request = new ApiServicePublishRequest();
        request.setVersion(version);
        request.setVersionDescription("版本 " + version + " 发布");
        request.setForcePublish(false);
        return request;
    }
    
    /**
     * 创建强制发布请求
     */
    public static ApiServicePublishRequest createForcePublishRequest(String version) {
        ApiServicePublishRequest request = createPublishRequest(version);
        request.setForcePublish(true);
        request.setVersionDescription("强制发布版本 " + version);
        return request;
    }
    
    /**
     * 创建表选择请求列表
     */
    public static List<TableSelectionRequest> createTableSelectionRequests() {
        TableSelectionRequest usersTable = new TableSelectionRequest();
        usersTable.setTableName("test_users");
        usersTable.setTableAlias("u");
        usersTable.setIsPrimary(true);
        usersTable.setSelectedColumns(Arrays.asList("id", "username", "email", "status"));
        usersTable.setSortOrder(0);
        
        TableSelectionRequest ordersTable = new TableSelectionRequest();
        ordersTable.setTableName("test_orders");
        ordersTable.setTableAlias("o");
        ordersTable.setIsPrimary(false);
        ordersTable.setJoinType("LEFT");
        ordersTable.setJoinCondition("u.id = o.user_id");
        ordersTable.setSelectedColumns(Arrays.asList("quantity", "total_price", "status", "order_date"));
        ordersTable.setSortOrder(1);
        
        return Arrays.asList(usersTable, ordersTable);
    }
    
    /**
     * 创建复杂的表选择请求列表
     */
    public static List<TableSelectionRequest> createComplexTableSelectionRequests() {
        TableSelectionRequest ordersTable = new TableSelectionRequest();
        ordersTable.setTableName("test_orders");
        ordersTable.setTableAlias("o");
        ordersTable.setIsPrimary(true);
        ordersTable.setSelectedColumns(Arrays.asList("id", "quantity", "unit_price", "total_price", "status"));
        ordersTable.setSortOrder(0);
        
        TableSelectionRequest usersTable = new TableSelectionRequest();
        usersTable.setTableName("test_users");
        usersTable.setTableAlias("u");
        usersTable.setIsPrimary(false);
        usersTable.setJoinType("INNER");
        usersTable.setJoinCondition("o.user_id = u.id");
        usersTable.setSelectedColumns(Arrays.asList("username", "email"));
        usersTable.setSortOrder(1);
        
        TableSelectionRequest productsTable = new TableSelectionRequest();
        productsTable.setTableName("test_products");
        productsTable.setTableAlias("p");
        productsTable.setIsPrimary(false);
        productsTable.setJoinType("INNER");
        productsTable.setJoinCondition("o.product_id = p.id");
        productsTable.setSelectedColumns(Arrays.asList("name", "description", "price"));
        productsTable.setSortOrder(2);
        
        return Arrays.asList(ordersTable, usersTable, productsTable);
    }
    
    /**
     * 创建API服务实体
     */
    public static ApiService createApiServiceEntity(Long userId) {
        ApiService apiService = new ApiService();
        apiService.setName("测试API服务");
        apiService.setDescription("用于测试的API服务");
        apiService.setPath("/api/test");
        apiService.setMethod(ApiService.HttpMethod.GET);
        apiService.setDataSourceId(1L);
        apiService.setSqlContent("SELECT * FROM test_users");
        apiService.setStatus(ApiStatus.DRAFT);
        apiService.setCacheEnabled(true);
        apiService.setCacheDuration(300);
        apiService.setRateLimit(100);
        apiService.setCreatedBy(userId);
        apiService.setUpdatedBy(userId);
        return apiService;
    }
    
    /**
     * 创建测试数据源
     */
    public static DataSource createTestDataSource(Long userId) {
        DataSource dataSource = new DataSource();
        dataSource.setName("测试数据源");
        dataSource.setDescription("H2测试数据源");
        dataSource.setType(org.duqiu.fly.autoapi.datasource.enums.DataSourceType.MYSQL);
        dataSource.setHost("localhost");
        dataSource.setPort(3306);
        dataSource.setDatabase("testdb");
        dataSource.setUsername("testuser");
        dataSource.setPassword("testpass");
        dataSource.setConnectionUrl("jdbc:h2:mem:datasource_test;MODE=MySQL");
        dataSource.setEnabled(true);
        dataSource.setCreatedBy(userId);
        dataSource.setUpdatedBy(userId);
        return dataSource;
    }
    
    /**
     * 创建测试参数
     */
    public static Map<String, Object> createTestParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("status", "ACTIVE");
        params.put("limit", 10);
        return params;
    }
    
    /**
     * 创建复杂测试参数
     */
    public static Map<String, Object> createComplexTestParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("orderStatus", "COMPLETED");
        params.put("userStatus", "ACTIVE");
        params.put("limit", 50);
        return params;
    }
    
    /**
     * 创建聚合查询参数
     */
    public static Map<String, Object> createAggregateQueryParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("minOrders", 0);
        return params;
    }
    
    /**
     * 创建批量测试参数组合
     */
    public static List<Map<String, Object>> createBatchTestParameters() {
        return Arrays.asList(
            Map.of("status", "ACTIVE"),
            Map.of("status", "INACTIVE"),
            createTestParameters(),
            createComplexTestParameters()
        );
    }
    
    // ===== 私有工具方法 =====
    
    private static String createRequestParamsJson() {
        return "{\n" +
               "  \"status\": {\n" +
               "    \"type\": \"string\",\n" +
               "    \"required\": true,\n" +
               "    \"description\": \"用户状态\",\n" +
               "    \"enum\": [\"ACTIVE\", \"INACTIVE\"]\n" +
               "  }\n" +
               "}";
    }
    
    private static String createComplexRequestParamsJson() {
        return "{\n" +
               "  \"orderStatus\": {\n" +
               "    \"type\": \"string\",\n" +
               "    \"required\": true,\n" +
               "    \"description\": \"订单状态\"\n" +
               "  },\n" +
               "  \"userStatus\": {\n" +
               "    \"type\": \"string\",\n" +
               "    \"required\": false,\n" +
               "    \"description\": \"用户状态\",\n" +
               "    \"default\": \"ACTIVE\"\n" +
               "  },\n" +
               "  \"limit\": {\n" +
               "    \"type\": \"integer\",\n" +
               "    \"required\": false,\n" +
               "    \"description\": \"查询限制\",\n" +
               "    \"default\": 10\n" +
               "  }\n" +
               "}";
    }
    
    private static String createResponseExampleJson() {
        return "{\n" +
               "  \"success\": true,\n" +
               "  \"data\": [\n" +
               "    {\n" +
               "      \"username\": \"testuser1\",\n" +
               "      \"product_name\": \"iPhone 15\",\n" +
               "      \"quantity\": 1,\n" +
               "      \"total_price\": 8999.00,\n" +
               "      \"status\": \"COMPLETED\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"count\": 1\n" +
               "}";
    }
}