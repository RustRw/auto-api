package org.duqiu.fly.autoapi.datasource.core;

import java.util.List;

/**
 * 数据库感知连接接口 - 用于支持多数据库的数据源
 */
public interface DatabaseAwareConnection extends DataSourceConnection {
    
    /**
     * 获取数据库列表
     */
    List<String> getDatabases();
    
    /**
     * 切换到指定数据库
     */
    void useDatabase(String database);
}