package org.duqiu.fly.autoapi.gateway;

import org.duqiu.fly.autoapi.AutoApiApplication;
import org.duqiu.fly.autoapi.api.model.ApiService;
import org.duqiu.fly.autoapi.api.repository.ApiServiceRepository;
import org.duqiu.fly.autoapi.common.enums.ApiStatus;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.duqiu.fly.autoapi.gateway.dto.ApiServiceInfo;
import org.duqiu.fly.autoapi.gateway.service.ApiServiceDiscoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 服务发现集成测试
 * 测试服务发现功能的完整流程
 */
@SpringBootTest(classes = AutoApiApplication.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ServiceDiscoveryIntegrationTest {

    @Autowired
    private ApiServiceDiscoveryService apiServiceDiscoveryService;

    @Autowired
    private ApiServiceRepository apiServiceRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    private Long testDataSourceId;

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

        // 初始服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();
    }

    @Test
    void testServiceDiscoveryWithNewPublishedService() {
        // 初始服务数量
        int initialCount = apiServiceDiscoveryService.getActiveServiceCount();

        // 创建新的已发布服务
        ApiService newService = TestServiceFactory.createUserQueryService(testDataSourceId, 1L);
        apiServiceRepository.save(newService);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证服务被发现
        int newCount = apiServiceDiscoveryService.getActiveServiceCount();
        assertEquals(initialCount + 1, newCount, "应该发现一个新服务");

        // 验证服务信息正确
        List<ApiServiceInfo> activeServices = apiServiceDiscoveryService.getActiveServices();
        ApiServiceInfo discoveredService = activeServices.stream()
                .filter(s -> s.getServiceId().equals(newService.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("新服务应该被发现"));

        assertEquals(newService.getName(), discoveredService.getServiceName());
        assertEquals(newService.getPath(), discoveredService.getServicePath());
        assertEquals(newService.getMethod().name(), discoveredService.getHttpMethod());
    }

    @Test
    void testServiceDiscoveryWithMultipleServices() {
        // 创建多个已发布服务
        ApiService service1 = TestServiceFactory.createUserQueryService(testDataSourceId, 1L);
        ApiService service2 = TestServiceFactory.createMultiParamService(testDataSourceId, 1L);
        ApiService service3 = TestServiceFactory.createProductService(testDataSourceId, 1L);

        apiServiceRepository.save(service1);
        apiServiceRepository.save(service2);
        apiServiceRepository.save(service3);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证所有服务都被发现
        List<ApiServiceInfo> activeServices = apiServiceDiscoveryService.getActiveServices();
        assertTrue(activeServices.size() >= 3, "应该发现至少3个服务");

        assertTrue(activeServices.stream().anyMatch(s -> s.getServiceId().equals(service1.getId())));
        assertTrue(activeServices.stream().anyMatch(s -> s.getServiceId().equals(service2.getId())));
        assertTrue(activeServices.stream().anyMatch(s -> s.getServiceId().equals(service3.getId())));
    }

    @Test
    void testServiceDiscoveryIgnoresDraftServices() {
        // 创建草稿服务
        ApiService draftService = TestServiceFactory.createDraftService(testDataSourceId, 1L);
        apiServiceRepository.save(draftService);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证草稿服务没有被发现
        List<ApiServiceInfo> activeServices = apiServiceDiscoveryService.getActiveServices();
        assertFalse(activeServices.stream().anyMatch(s -> s.getServiceId().equals(draftService.getId())),
                "草稿服务不应该被发现");
    }

    @Test
    void testServiceDiscoveryWithServiceRemoval() {
        // 创建并发布一个服务
        ApiService service = TestServiceFactory.createUserQueryService(testDataSourceId, 1L);
        service = apiServiceRepository.save(service);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证服务被发现
        List<ApiServiceInfo> activeServices = apiServiceDiscoveryService.getActiveServices();
        final Long serviceId = service.getId();
        assertTrue(activeServices.stream().anyMatch(s -> s.getServiceId().equals(serviceId)),
                "服务应该被发现");

        // 删除服务
        apiServiceRepository.delete(service);

        // 再次执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证服务被移除
        List<ApiServiceInfo> updatedServices = apiServiceDiscoveryService.getActiveServices();
        assertFalse(updatedServices.stream().anyMatch(s -> s.getServiceId().equals(serviceId)),
                "服务应该被移除");
    }

    @Test
    void testServiceDiscoveryWithServiceUpdate() {
        // 创建并发布一个服务
        ApiService service = TestServiceFactory.createUserQueryService(testDataSourceId, 1L);
        service = apiServiceRepository.save(service);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 更新服务
        String originalName = service.getName();
        service.setName("更新后的服务名称");
        service.setDescription("更新后的描述");
        apiServiceRepository.save(service);

        // 再次执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证服务信息已更新
        List<ApiServiceInfo> activeServices = apiServiceDiscoveryService.getActiveServices();
        final Long serviceId2 = service.getId();
        ApiServiceInfo updatedService = activeServices.stream()
                .filter(s -> s.getServiceId().equals(serviceId2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("服务应该存在"));

        assertEquals("更新后的服务名称", updatedService.getServiceName());
        assertNotEquals(originalName, updatedService.getServiceName());
    }

    @Test
    void testServiceDiscoveryWithUnpublishing() {
        // 创建并发布一个服务
        ApiService service = TestServiceFactory.createUserQueryService(testDataSourceId, 1L);
        service = apiServiceRepository.save(service);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 取消发布服务
        service.setStatus(ApiStatus.DRAFT);
        apiServiceRepository.save(service);

        // 再次执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证服务被移除
        List<ApiServiceInfo> activeServices = apiServiceDiscoveryService.getActiveServices();
        final Long serviceId3 = service.getId();
        assertFalse(activeServices.stream().anyMatch(s -> s.getServiceId().equals(serviceId3)),
                "取消发布的服务应该被移除");
    }

    @Test
    void testServiceDiscoveryStatistics() {
        // 初始统计
        var initialStats = apiServiceDiscoveryService.getChangeStatistics();

        // 创建多个服务
        ApiService service1 = TestServiceFactory.createUserQueryService(testDataSourceId, 1L);
        ApiService service2 = TestServiceFactory.createMultiParamService(testDataSourceId, 1L);
        apiServiceRepository.save(service1);
        apiServiceRepository.save(service2);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证统计信息
        var stats = apiServiceDiscoveryService.getChangeStatistics();
        assertEquals(2, stats.get("added"), "应该统计2个新增服务");
        assertEquals(0, stats.get("updated"), "应该没有更新服务");
        assertEquals(0, stats.get("removed"), "应该没有移除服务");

        // 更新一个服务
        service1.setName("更新后的名称");
        apiServiceRepository.save(service1);

        // 再次执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证更新统计
        var updatedStats = apiServiceDiscoveryService.getChangeStatistics();
        assertEquals(2, updatedStats.get("added"), "新增统计应该保持不变");
        assertEquals(1, updatedStats.get("updated"), "应该统计1个更新服务");

        // 重置统计
        apiServiceDiscoveryService.resetChangeStatistics();
        var resetStats = apiServiceDiscoveryService.getChangeStatistics();
        assertEquals(0, resetStats.get("added"));
        assertEquals(0, resetStats.get("updated"));
        assertEquals(0, resetStats.get("removed"));
    }

    @Test
    void testGetServiceByPathAndMethod() {
        // 创建服务
        ApiService service = TestServiceFactory.createUserQueryService(testDataSourceId, 1L);
        apiServiceRepository.save(service);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 验证通过路径和方法获取服务
        ApiServiceInfo foundService = apiServiceDiscoveryService.getService(
                service.getMethod().name(),
                service.getPath()
        );

        assertNotNull(foundService, "应该能找到服务");
        assertEquals(service.getId(), foundService.getServiceId());
        assertEquals(service.getName(), foundService.getServiceName());
    }

    @Test
    void testGetServiceByKey() {
        // 创建服务
        ApiService service = TestServiceFactory.createUserQueryService(testDataSourceId, 1L);
        apiServiceRepository.save(service);

        // 执行服务发现
        apiServiceDiscoveryService.discoverAndUpdateServices();

        // 生成服务键
        String serviceKey = String.format("%s:%s:%s", 
                service.getMethod().name(),
                service.getPath(),
                "1.0" // 假设版本为1.0
        );

        // 验证通过服务键获取服务
        ApiServiceInfo foundService = apiServiceDiscoveryService.getService(serviceKey);
        assertNotNull(foundService, "应该能找到服务");
        assertEquals(service.getId(), foundService.getServiceId());
    }
}