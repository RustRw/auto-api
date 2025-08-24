package org.duqiu.fly.autoapi.datasource.metadata;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;

import java.util.List;
import java.util.Map;

/**
 * 元数据适配器接口 - 为不同数据源类型提供统一的元数据访问接口
 */
public interface MetadataAdapter {
    
    /**
     * 获取数据库列表
     */
    List<String> getDatabases(DataSourceConnection connection);
    
    /**
     * 获取模式/命名空间列表
     */
    List<String> getSchemas(DataSourceConnection connection, String database);
    
    /**
     * 获取表/集合列表
     */
    List<DataSourceConnection.TableInfo> getTables(DataSourceConnection connection, String database, String schema);
    
    /**
     * 获取表结构信息
     */
    DataSourceConnection.TableSchema getTableSchema(DataSourceConnection connection, String tableName, String database, String schema);
    
    /**
     * 获取表统计信息
     */
    Map<String, Object> getTableStatistics(DataSourceConnection connection, String tableName, String database, String schema);
    
    /**
     * 获取表样本数据
     */
    List<Map<String, Object>> getTableSampleData(DataSourceConnection connection, String tableName, String database, String schema, int limit);
    
    /**
     * 验证表或集合是否存在
     */
    boolean tableExists(DataSourceConnection connection, String tableName, String database, String schema);
}