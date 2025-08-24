package org.duqiu.fly.autoapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "org.duqiu.fly.autoapi")
@EnableJpaAuditing
public class JpaConfig {
}