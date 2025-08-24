package org.duqiu.fly.autoapi.datasource.integration;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 数据源集成测试套件 - 运行所有集成测试
 */
@Suite
@SuiteDisplayName("DataSource API 集成测试套件")
@SelectClasses({
    DataSourceCrudIntegrationTest.class,
    DataSourceConnectionIntegrationTest.class,
    MetadataQueryIntegrationTest.class,
    TableSchemaIntegrationTest.class,
    QueryExecutionIntegrationTest.class
})
public class DataSourceIntegrationTestSuite {
    // 测试套件类，无需实现任何方法
}