package org.duqiu.fly.autoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * API服务测试应用程序入口
 */
@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    MongoRepositoriesAutoConfiguration.class,
    ElasticsearchClientAutoConfiguration.class,
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class
})
public class ApiServiceTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiServiceTestApplication.class, args);
    }
}