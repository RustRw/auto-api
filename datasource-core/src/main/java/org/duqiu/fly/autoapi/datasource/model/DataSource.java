package org.duqiu.fly.autoapi.datasource.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.duqiu.fly.autoapi.common.model.TenantAwareBaseEntity;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;

@Entity
@Table(name = "data_sources")
@Data
@EqualsAndHashCode(callSuper = true)
public class DataSource extends TenantAwareBaseEntity {
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataSourceType type;
    
    @Column(nullable = false, length = 200)
    private String host;
    
    @Column(nullable = false)
    private Integer port;
    
    @Column(length = 100)
    private String database;
    
    @Column(length = 100)
    private String username;
    
    @Column(length = 500)
    private String password;
    
    @Column(name = "connection_url", length = 1000)
    private String connectionUrl;
    
    @Column(name = "max_pool_size")
    private Integer maxPoolSize = 10;
    
    @Column(name = "connection_timeout")
    private Integer connectionTimeout = 30000;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(name = "test_query", length = 500)
    private String testQuery;
    
    @Column(length = 50)
    private String version;
    
    @Column(name = "additional_properties", length = 2000)
    private String additionalProperties;
    
    @Column(name = "ssl_enabled")
    private Boolean sslEnabled = false;
    
    @Column(name = "connection_pool_enabled")
    private Boolean connectionPoolEnabled = true;
    
    @Column(name = "min_pool_size")
    private Integer minPoolSize = 1;
    
    @Column(name = "idle_timeout")
    private Integer idleTimeout = 600000;
    
    @Column(name = "max_lifetime")
    private Integer maxLifetime = 1800000;
}