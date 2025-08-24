package org.duqiu.fly.autoapi.multitenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duqiu.fly.autoapi.auth.dto.LoginRequest;
import org.duqiu.fly.autoapi.auth.dto.LoginResponse;
import org.duqiu.fly.autoapi.auth.dto.RegisterRequest;
import org.duqiu.fly.autoapi.auth.model.User;
import org.duqiu.fly.autoapi.auth.repository.TenantRepository;
import org.duqiu.fly.autoapi.auth.repository.UserRepository;
import org.duqiu.fly.autoapi.common.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * 多租户集成测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MultiTenantIntegrationTest {

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private TenantRepository tenantRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private MockMvc mockMvc;
    
    private Tenant tenant1;
    private Tenant tenant2;
    private User tenant1User;
    private User tenant2User;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        setupTestData();
    }
    
    @Transactional
    void setupTestData() {
        // 创建两个测试租户
        tenant1 = new Tenant();
        tenant1.setTenantCode("tenant1");
        tenant1.setTenantName("Tenant 1");
        tenant1.setDescription("Test Tenant 1");
        tenant1.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant1.setCreatedBy(1L);
        tenant1.setUpdatedBy(1L);
        tenant1 = tenantRepository.save(tenant1);
        
        tenant2 = new Tenant();
        tenant2.setTenantCode("tenant2");
        tenant2.setTenantName("Tenant 2");
        tenant2.setDescription("Test Tenant 2");
        tenant2.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant2.setCreatedBy(1L);
        tenant2.setUpdatedBy(1L);
        tenant2 = tenantRepository.save(tenant2);
        
        // 为每个租户创建用户
        tenant1User = new User();
        tenant1User.setUsername("user1");
        tenant1User.setPassword(passwordEncoder.encode("password"));
        tenant1User.setEmail("user1@tenant1.com");
        tenant1User.setTenantId(tenant1.getId());
        tenant1User.setEnabled(true);
        tenant1User.setRole(User.Role.USER);
        tenant1User = userRepository.save(tenant1User);
        
        tenant2User = new User();
        tenant2User.setUsername("user2");
        tenant2User.setPassword(passwordEncoder.encode("password"));
        tenant2User.setEmail("user2@tenant2.com");
        tenant2User.setTenantId(tenant2.getId());
        tenant2User.setEnabled(true);
        tenant2User.setRole(User.Role.USER);
        tenant2User = userRepository.save(tenant2User);
    }
    
    @Test
    void testTenantIsolationInAuthentication() throws Exception {
        // 测试用户1登录
        LoginRequest loginRequest1 = new LoginRequest();
        loginRequest1.setUsername("user1");
        loginRequest1.setPassword("password");
        
        String response1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest1)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        LoginResponse loginResponse1 = objectMapper.readValue(response1, LoginResponse.class);
        
        // 测试用户2登录
        LoginRequest loginRequest2 = new LoginRequest();
        loginRequest2.setUsername("user2");
        loginRequest2.setPassword("password");
        
        String response2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest2)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        LoginResponse loginResponse2 = objectMapper.readValue(response2, LoginResponse.class);
        
        // 验证两个用户获得了不同的token
        assert !loginResponse1.getToken().equals(loginResponse2.getToken());
        
        // 使用用户1的token访问租户信息
        mockMvc.perform(get("/api/v1/tenants/current")
                        .header("Authorization", "Bearer " + loginResponse1.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantCode").value("tenant1"));
        
        // 使用用户2的token访问租户信息
        mockMvc.perform(get("/api/v1/tenants/current")
                        .header("Authorization", "Bearer " + loginResponse2.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantCode").value("tenant2"));
    }
    
    @Test
    void testDataSourceTenantIsolation() throws Exception {
        // 这里可以添加数据源隔离的测试
        // 由于当前数据源服务尚未完全更新，这里先留空
    }
    
    @Test
    void testUserCannotAccessOtherTenantData() throws Exception {
        // 测试用户无法访问其他租户的数据
        // 这里可以添加跨租户访问控制的测试
    }
}