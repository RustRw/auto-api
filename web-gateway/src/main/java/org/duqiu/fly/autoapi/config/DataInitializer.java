package org.duqiu.fly.autoapi.config;

import org.duqiu.fly.autoapi.auth.model.User;
import org.duqiu.fly.autoapi.auth.repository.UserRepository;
import org.duqiu.fly.autoapi.auth.repository.TenantRepository;
import org.duqiu.fly.autoapi.common.model.Tenant;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.repository.ApiServiceRepository;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final DataSourceRepository dataSourceRepository;
    private final ApiServiceRepository apiServiceRepository;
    private final PasswordEncoder passwordEncoder;
    
    public DataInitializer(UserRepository userRepository, TenantRepository tenantRepository,
                          DataSourceRepository dataSourceRepository, ApiServiceRepository apiServiceRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.apiServiceRepository = apiServiceRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public void run(String... args) {
        // 首先创建默认租户
        Tenant defaultTenant = null;
        if (!tenantRepository.existsByTenantCode("default")) {
            defaultTenant = new Tenant();
            defaultTenant.setTenantCode("default");
            defaultTenant.setTenantName("Default Tenant");
            defaultTenant.setDescription("Default tenant for system initialization");
            defaultTenant.setStatus(Tenant.TenantStatus.ACTIVE);
            defaultTenant.setCreatedAt(LocalDateTime.now());
            defaultTenant = tenantRepository.save(defaultTenant);
            
            System.out.println("默认租户已创建: default");
        } else {
            defaultTenant = tenantRepository.findByTenantCode("default").orElse(null);
        }
        
        if (defaultTenant == null) {
            System.err.println("无法获取默认租户，用户创建失败");
            return;
        }
        
        // 创建管理员账户
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setRealName("Administrator");
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            admin.setTenantId(defaultTenant.getId());
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);
            
            System.out.println("默认管理员账户已创建:");
            System.out.println("用户名: admin");
            System.out.println("密码: admin123");
        }
        
        // 创建普通用户账户
        if (!userRepository.existsByUsername("user")) {
            User user = new User();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setEmail("user@example.com");
            user.setRealName("Test User");
            user.setRole(User.Role.USER);
            user.setEnabled(true);
            user.setTenantId(defaultTenant.getId());
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            System.out.println("默认用户账户已创建:");
            System.out.println("用户名: user");
            System.out.println("密码: user123");
        }
        
        // 创建默认数据源 (H2数据库)
        createDefaultDataSource(defaultTenant);
        
        // 创建示例API服务
        createSampleApiServices(defaultTenant);
    }
    
    private void createDefaultDataSource(Tenant tenant) {
        if (!dataSourceRepository.existsByNameAndTenantId("默认H2数据库", tenant.getId())) {
            DataSource h2DataSource = new DataSource();
            h2DataSource.setName("默认H2数据库");
            h2DataSource.setDescription("系统内置的H2内存数据库，包含用户、租户等系统数据");
            h2DataSource.setType(DataSourceType.H2);
            h2DataSource.setHost("localhost");
            h2DataSource.setPort(8080); // H2 console通过web端口访问
            h2DataSource.setDatabase("autoapi");
            h2DataSource.setUsername("SA");
            h2DataSource.setPassword("");
            h2DataSource.setConnectionUrl("jdbc:h2:mem:autoapi");
            h2DataSource.setMaxPoolSize(10);
            h2DataSource.setConnectionTimeout(30000);
            h2DataSource.setEnabled(true);
            h2DataSource.setTenantId(tenant.getId());
            h2DataSource.setCreatedAt(LocalDateTime.now());
            h2DataSource.setCreatedBy(1L); // admin用户ID
            
            dataSourceRepository.save(h2DataSource);
            
            System.out.println("默认H2数据源已创建");
        }
    }
    
    private void createSampleApiServices(Tenant tenant) {
        // 获取默认数据源
        DataSource defaultDataSource = dataSourceRepository.findByNameAndTenantId("默认H2数据库", tenant.getId()).orElse(null);
        if (defaultDataSource == null) {
            System.err.println("无法找到默认数据源，跳过API创建");
            return;
        }
        
        // 创建用户列表API
        if (!apiServiceRepository.existsByNameAndTenantId("获取用户列表", tenant.getId())) {
            ApiService userListApi = new ApiService();
            userListApi.setName("获取用户列表");
            userListApi.setDescription("获取系统中所有用户的列表信息");
            userListApi.setPath("/api/demo/users");
            userListApi.setMethod(ApiService.HttpMethod.GET);
            userListApi.setDataSourceId(defaultDataSource.getId());
            userListApi.setSqlContent("SELECT id, username, email, real_name as realName, role, enabled, created_at as createdAt FROM users WHERE enabled = true ORDER BY created_at DESC");
            userListApi.setRequestParams("[]");
            userListApi.setResponseExample("[\n  {\n    \"id\": 1,\n    \"username\": \"admin\",\n    \"email\": \"admin@example.com\",\n    \"realName\": \"Administrator\",\n    \"role\": \"ADMIN\",\n    \"enabled\": true,\n    \"createdAt\": \"2024-08-24T15:25:46.000\"\n  }\n]");
            userListApi.setStatus(ApiStatus.PUBLISHED);
            userListApi.setEnabled(true);
            userListApi.setCacheEnabled(true);
            userListApi.setCacheDuration(300); // 5分钟缓存
            userListApi.setTenantId(tenant.getId());
            userListApi.setCreatedAt(LocalDateTime.now());
            userListApi.setCreatedBy(1L); // admin用户ID
            
            apiServiceRepository.save(userListApi);
            System.out.println("示例API已创建: 获取用户列表 - " + userListApi.getPath());
        }
        
        // 创建租户信息API
        if (!apiServiceRepository.existsByNameAndTenantId("获取租户信息", tenant.getId())) {
            ApiService tenantInfoApi = new ApiService();
            tenantInfoApi.setName("获取租户信息");
            tenantInfoApi.setDescription("获取当前租户的详细信息");
            tenantInfoApi.setPath("/api/demo/tenant");
            tenantInfoApi.setMethod(ApiService.HttpMethod.GET);
            tenantInfoApi.setDataSourceId(defaultDataSource.getId());
            tenantInfoApi.setSqlContent("SELECT id, tenant_code as tenantCode, tenant_name as tenantName, description, status, max_users as maxUsers, max_datasources as maxDataSources, max_api_services as maxApiServices, created_at as createdAt FROM tenants WHERE tenant_code = 'default'");
            tenantInfoApi.setRequestParams("[]");
            tenantInfoApi.setResponseExample("{\n  \"id\": 1,\n  \"tenantCode\": \"default\",\n  \"tenantName\": \"Default Tenant\",\n  \"description\": \"Default tenant for system initialization\",\n  \"status\": \"ACTIVE\",\n  \"maxUsers\": 10,\n  \"maxDataSources\": 5,\n  \"maxApiServices\": 20,\n  \"createdAt\": \"2024-08-24T15:25:45.000\"\n}");
            tenantInfoApi.setStatus(ApiStatus.PUBLISHED);
            tenantInfoApi.setEnabled(true);
            tenantInfoApi.setCacheEnabled(true);
            tenantInfoApi.setCacheDuration(600); // 10分钟缓存
            tenantInfoApi.setTenantId(tenant.getId());
            tenantInfoApi.setCreatedAt(LocalDateTime.now());
            tenantInfoApi.setCreatedBy(1L); // admin用户ID
            
            apiServiceRepository.save(tenantInfoApi);
            System.out.println("示例API已创建: 获取租户信息 - " + tenantInfoApi.getPath());
        }
        
        // 创建统计信息API
        if (!apiServiceRepository.existsByNameAndTenantId("系统统计信息", tenant.getId())) {
            ApiService statsApi = new ApiService();
            statsApi.setName("系统统计信息");
            statsApi.setDescription("获取系统的统计信息，包括用户数量、数据源数量等");
            statsApi.setPath("/api/demo/stats");
            statsApi.setMethod(ApiService.HttpMethod.GET);
            statsApi.setDataSourceId(defaultDataSource.getId());
            statsApi.setSqlContent("SELECT \n  (SELECT COUNT(*) FROM users WHERE enabled = true) as userCount,\n  (SELECT COUNT(*) FROM data_sources WHERE enabled = true) as dataSourceCount,\n  (SELECT COUNT(*) FROM api_services WHERE enabled = true) as apiServiceCount,\n  (SELECT COUNT(*) FROM tenants WHERE status = 'ACTIVE') as tenantCount");
            statsApi.setRequestParams("[]");
            statsApi.setResponseExample("{\n  \"userCount\": 2,\n  \"dataSourceCount\": 1,\n  \"apiServiceCount\": 3,\n  \"tenantCount\": 1\n}");
            statsApi.setStatus(ApiStatus.PUBLISHED);
            statsApi.setEnabled(true);
            statsApi.setCacheEnabled(true);
            statsApi.setCacheDuration(180); // 3分钟缓存
            statsApi.setTenantId(tenant.getId());
            statsApi.setCreatedAt(LocalDateTime.now());
            statsApi.setCreatedBy(1L); // admin用户ID
            
            apiServiceRepository.save(statsApi);
            System.out.println("示例API已创建: 系统统计信息 - " + statsApi.getPath());
        }
        
        System.out.println("所有示例数据创建完成！");
        System.out.println("您可以通过以下方式测试API:");
        System.out.println("- 访问: http://localhost:8080/api/demo/users");
        System.out.println("- 访问: http://localhost:8080/api/demo/tenant");  
        System.out.println("- 访问: http://localhost:8080/api/demo/stats");
    }
}