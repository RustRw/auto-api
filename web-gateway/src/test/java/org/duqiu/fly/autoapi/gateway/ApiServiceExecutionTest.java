package org.duqiu.fly.autoapi.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duqiu.fly.autoapi.AutoApiApplication;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.repository.ApiServiceRepository;
import org.duqiu.fly.autoapi.api.repository.ApiServiceVersionRepository;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.duqiu.fly.autoapi.gateway.service.ApiServiceDiscoveryService;
import org.duqiu.fly.autoapi.test.dto.ApiTestRequest;
import org.duqiu.fly.autoapi.test.service.ApiTestingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * API服务执行测试
 * 针对已发布的服务进行参数化请求测试
 */
@SpringBootTest(classes = AutoApiApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ApiServiceExecutionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApiServiceRepository apiServiceRepository;

    @Autowired
    private ApiServiceVersionRepository apiServiceVersionRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private ApiTestingService apiTestingService;

    @Autowired
    private ApiServiceDiscoveryService apiServiceDiscoveryService;

    private Long testDataSourceId;
    private Long publishedServiceId;

    @BeforeEach
    void setUp() {
        // 创建测试数据源
        DataSource dataSource = new DataSource();
        dataSource.setName("Test H2 Database");
        dataSource.setType(org.duqiu.fly.autoapi.datasource.enums.DataSourceType.H2);
        dataSource.setHost("localhost");
        dataSource.setPort(9092);
        dataSource.setDatabase("testdb");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setConnectionUrl("jdbc:h2:mem:testdb");
        dataSource.setCreatedBy(1L);
        dataSource = dataSourceRepository.save(dataSource);
        testDataSourceId = dataSource.getId();

        // 创建已发布的API服务
        ApiService apiService = new ApiService();
        apiService.setName("用户查询服务");
        apiService.setDescription("根据用户ID查询用户信息");
        apiService.setPath("/api/users/{userId}");
        apiService.setMethod(ApiService.HttpMethod.GET);
        apiService.setDataSourceId(testDataSourceId);
        apiService.setSqlContent("SELECT * FROM users WHERE id = ${userId}");
        apiService.setRequestParams("{\"userId\": {\"type\": \"number\", \"required\": true, \"description\": \"用户ID\"}}");
        apiService.setStatus(ApiStatus.PUBLISHED);
        apiService.setEnabled(true);
        apiService.setCreatedBy(1L);
        apiService = apiServiceRepository.save(apiService);
        publishedServiceId = apiService.getId();

        // 触发服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();
    }

    @Test
    void testExecutePublishedApiWithValidParameters() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 1);

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(publishedServiceId);
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.executionTime").isNumber());
    }

    @Test
    void testExecutePublishedApiWithMissingRequiredParameters() throws Exception {
        Map<String, Object> params = new HashMap<>();
        // 故意不提供必需的userId参数

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(publishedServiceId);
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    void testExecutePublishedApiWithInvalidParameterTypes() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", "invalid_string"); // 应该是数字类型

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(publishedServiceId);
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    void testExecutePublishedApiWithMultipleParameters() throws Exception {
        // 创建支持多个参数的服务
        ApiService multiParamService = new ApiService();
        multiParamService.setName("多参数查询服务");
        multiParamService.setDescription("根据多个条件查询用户");
        multiParamService.setPath("/api/users/search");
        multiParamService.setMethod(ApiService.HttpMethod.POST);
        multiParamService.setDataSourceId(testDataSourceId);
        multiParamService.setSqlContent("SELECT * FROM users WHERE name LIKE ${name} AND age > ${minAge}");
        multiParamService.setRequestParams("{\"name\": {\"type\": \"string\", \"required\": false}, \"minAge\": {\"type\": \"number\", \"required\": false}}");
        multiParamService.setStatus(ApiStatus.PUBLISHED);
        multiParamService.setEnabled(true);
        multiParamService.setCreatedBy(1L);
        multiParamService = apiServiceRepository.save(multiParamService);

        Map<String, Object> params = new HashMap<>();
        params.put("name", "%John%");
        params.put("minAge", 18);

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(multiParamService.getId());
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testExecutePublishedApiWithOptionalParameters() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 1);
        // 可选参数不提供

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(publishedServiceId);
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testExecutePublishedApiWithPathParameters() throws Exception {
        // 测试路径参数的服务
        ApiService pathParamService = new ApiService();
        pathParamService.setName("路径参数服务");
        pathParamService.setDescription("使用路径参数查询");
        pathParamService.setPath("/api/users/{userId}/details");
        pathParamService.setMethod(ApiService.HttpMethod.GET);
        pathParamService.setDataSourceId(testDataSourceId);
        pathParamService.setSqlContent("SELECT * FROM user_details WHERE user_id = ${userId}");
        pathParamService.setRequestParams("{\"userId\": {\"type\": \"number\", \"required\": true, \"in\": \"path\"}}");
        pathParamService.setStatus(ApiStatus.PUBLISHED);
        pathParamService.setEnabled(true);
        pathParamService.setCreatedBy(1L);
        pathParamService = apiServiceRepository.save(pathParamService);

        Map<String, Object> params = new HashMap<>();
        params.put("userId", 123);

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(pathParamService.getId());
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testExecutePublishedApiWithQueryParameters() throws Exception {
        // 测试查询参数的服务
        ApiService queryParamService = new ApiService();
        queryParamService.setName("查询参数服务");
        queryParamService.setDescription("使用查询参数过滤");
        queryParamService.setPath("/api/products");
        queryParamService.setMethod(ApiService.HttpMethod.GET);
        queryParamService.setDataSourceId(testDataSourceId);
        queryParamService.setSqlContent("SELECT * FROM products WHERE category = ${category} AND price <= ${maxPrice}");
        queryParamService.setRequestParams("{\"category\": {\"type\": \"string\", \"required\": true, \"in\": \"query\"}, \"maxPrice\": {\"type\": \"number\", \"required\": false, \"in\": \"query\"}}");
        queryParamService.setStatus(ApiStatus.PUBLISHED);
        queryParamService.setEnabled(true);
        queryParamService.setCreatedBy(1L);
        queryParamService = apiServiceRepository.save(queryParamService);

        Map<String, Object> params = new HashMap<>();
        params.put("category", "electronics");
        params.put("maxPrice", 1000);

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(queryParamService.getId());
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testExecutePublishedApiWithBodyParameters() throws Exception {
        // 测试Body参数的服务
        ApiService bodyParamService = new ApiService();
        bodyParamService.setName("Body参数服务");
        bodyParamService.setDescription("使用JSON body参数");
        bodyParamService.setPath("/api/users/create");
        bodyParamService.setMethod(ApiService.HttpMethod.POST);
        bodyParamService.setDataSourceId(testDataSourceId);
        bodyParamService.setSqlContent("INSERT INTO users (name, email, age) VALUES (${name}, ${email}, ${age})");
        bodyParamService.setRequestParams("{\"name\": {\"type\": \"string\", \"required\": true, \"in\": \"body\"}, \"email\": {\"type\": \"string\", \"required\": true, \"in\": \"body\"}, \"age\": {\"type\": \"number\", \"required\": false, \"in\": \"body\"}}");
        bodyParamService.setStatus(ApiStatus.PUBLISHED);
        bodyParamService.setEnabled(true);
        bodyParamService.setCreatedBy(1L);
        bodyParamService = apiServiceRepository.save(bodyParamService);

        Map<String, Object> params = new HashMap<>();
        params.put("name", "John Doe");
        params.put("email", "john@example.com");
        params.put("age", 30);

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(bodyParamService.getId());
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testExecuteNonExistentApi() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("userId", 1);

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(9999L); // 不存在的服务ID
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").value("API服务不存在"));
    }

    @Test
    void testExecuteUnpublishedApi() throws Exception {
        // 创建未发布的服务
        ApiService unpublishedService = new ApiService();
        unpublishedService.setName("未发布服务");
        unpublishedService.setDescription("这是一个未发布的服务");
        unpublishedService.setPath("/api/unpublished");
        unpublishedService.setMethod(ApiService.HttpMethod.GET);
        unpublishedService.setDataSourceId(testDataSourceId);
        unpublishedService.setSqlContent("SELECT 1");
        unpublishedService.setStatus(ApiStatus.DRAFT);
        unpublishedService.setEnabled(true);
        unpublishedService.setCreatedBy(1L);
        unpublishedService = apiServiceRepository.save(unpublishedService);

        Map<String, Object> params = new HashMap<>();

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(unpublishedService.getId());
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    void testExecuteApiWithMalformedSql() throws Exception {
        // 创建包含错误SQL的服务
        ApiService malformedService = new ApiService();
        malformedService.setName("错误SQL服务");
        malformedService.setDescription("包含语法错误的SQL");
        malformedService.setPath("/api/malformed");
        malformedService.setMethod(ApiService.HttpMethod.GET);
        malformedService.setDataSourceId(testDataSourceId);
        malformedService.setSqlContent("SELECT FROM WHERE"); // 错误的SQL
        malformedService.setRequestParams("{}");
        malformedService.setStatus(ApiStatus.PUBLISHED);
        malformedService.setEnabled(true);
        malformedService.setCreatedBy(1L);
        malformedService = apiServiceRepository.save(malformedService);

        Map<String, Object> params = new HashMap<>();

        ApiTestRequest request = new ApiTestRequest();
        request.setApiServiceId(malformedService.getId());
        request.setParameters(params);

        mockMvc.perform(post("/api/testing/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorMessage").exists());
    }

    @Test
    void testServiceDiscoveryIntegration() throws Exception {
        // 验证服务发现功能正常工作
        int initialCount = apiServiceDiscoveryService.getActiveServiceCount();
        
        // 创建新的已发布服务
        ApiService newService = new ApiService();
        newService.setName("新测试服务");
        newService.setDescription("用于测试服务发现");
        newService.setPath("/api/discovery-test");
        newService.setMethod(ApiService.HttpMethod.GET);
        newService.setDataSourceId(testDataSourceId);
        newService.setSqlContent("SELECT 1");
        newService.setRequestParams("{}");
        newService.setStatus(ApiStatus.PUBLISHED);
        newService.setEnabled(true);
        newService.setCreatedBy(1L);
        apiServiceRepository.save(newService);

        // 触发服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证服务被发现
        int newCount = apiServiceDiscoveryService.getActiveServiceCount();
        assert newCount > initialCount : "服务发现应该检测到新服务";
    }
}