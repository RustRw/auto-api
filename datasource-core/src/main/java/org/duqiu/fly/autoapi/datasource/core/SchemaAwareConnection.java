package org.duqiu.fly.autoapi.datasource.core;

import java.util.List;

/**
 * 模式感知连接接口 - 用于支持多模式（Schema）的数据源
 */
public interface SchemaAwareConnection extends DataSourceConnection {
    
    /**
     * 获取模式列表
     */
    List<String> getSchemas();
    
    /**
     * 获取指定数据库和模式下的表列表
     */
    List<TableInfo> getTables(String database, String schema);
    
    /**
     * 获取指定表的结构信息（支持数据库和模式参数）
     */
    TableSchema getTableSchema(String tableName, String database, String schema);
}