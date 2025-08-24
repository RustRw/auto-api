package org.duqiu.fly.autoapi.datasource.enums;

public enum DataSourceCategory {
    RELATIONAL_DB("关系型数据库"),
    NOSQL_DB("NoSQL数据库"),
    TIME_SERIES_DB("时序数据库"),
    GRAPH_DB("图数据库"),
    ANALYTICAL_DB("分析型数据库"),
    MESSAGE_QUEUE("消息队列"),
    HTTP_API("HTTP接口"),
    CACHE("缓存");

    private final String displayName;

    DataSourceCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}