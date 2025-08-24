package org.duqiu.fly.autoapi.datasource.dto;

import lombok.Data;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;

import java.time.LocalDateTime;

@Data
public class DataSourceResponse {
    private Long id;
    private String name;
    private String description;
    private DataSourceType type;
    private String host;
    private Integer port;
    private String database;
    private String username;
    private Integer maxPoolSize;
    private Integer connectionTimeout;
    private Boolean enabled;
    private String testQuery;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}