package org.duqiu.fly.autoapi.service.integration;

import org.duqiu.fly.autoapi.test.integration.ApiTestingIntegrationTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * API服务核心模块集成测试套件
 */
@Suite
@SuiteDisplayName("API服务核心模块集成测试套件")
@SelectClasses({
    ApiServiceCrudIntegrationTest.class,
    ApiServiceVersionIntegrationTest.class,
    ApiTestingIntegrationTest.class,
    TableSelectionIntegrationTest.class,
    AuditLogIntegrationTest.class,
    FullApiServiceWorkflowIntegrationTest.class
})
public class ApiServiceIntegrationTestSuite {
    // 测试套件类，无需实现任何方法
}