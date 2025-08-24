package org.duqiu.fly.autoapi.datasource.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.metadata.MetadataService;
import org.duqiu.fly.autoapi.datasource.service.EnhancedDataSourceService;
import org.duqiu.fly.autoapi.datasource.validation.DataSourceValidator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 集成测试配置类
 */
@TestConfiguration
public class IntegrationTestConfiguration {
    
    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        return new ObjectMapper();
    }
    
    @Bean
    public DataSourceValidator dataSourceValidator() {
        return new DataSourceValidator();
    }
    
    @Bean
    public UnifiedDataSourceFactory unifiedDataSourceFactory() {
        return new UnifiedDataSourceFactory();
    }
    
    @Bean
    public MetadataService metadataService(UnifiedDataSourceFactory dataSourceFactory) {
        return new MetadataService(dataSourceFactory);
    }
}