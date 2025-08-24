package org.duqiu.fly.autoapi.datasource.core;

import java.util.List;
import java.util.Map;

/**
 * 数据源连接接口 - 统一不同类型数据源的操作
 */
public interface DataSourceConnection extends AutoCloseable {
    
    /**
     * 测试连接是否有效
     */
    boolean isValid();
    
    /**
     * 执行查询操作
     */
    QueryResult executeQuery(String query, Map<String, Object> parameters);
    
    /**
     * 执行更新操作
     */
    UpdateResult executeUpdate(String command, Map<String, Object> parameters);
    
    /**
     * 获取连接信息
     */
    ConnectionInfo getConnectionInfo();
    
    /**
     * 获取表/集合列表
     */
    List<TableInfo> getTables();
    
    /**
     * 获取表结构信息
     */
    TableSchema getTableSchema(String tableName);
    
    /**
     * 关闭连接
     */
    @Override
    void close();
    
    /**
     * 查询结果
     */
    interface QueryResult {
        List<Map<String, Object>> getData();
        long getCount();
        List<ColumnInfo> getColumns();
        long getExecutionTime();
        boolean isSuccess();
        String getErrorMessage();
    }
    
    /**
     * 更新结果
     */
    interface UpdateResult {
        long getAffectedRows();
        long getExecutionTime();
        boolean isSuccess();
        String getErrorMessage();
    }
    
    /**
     * 连接信息
     */
    interface ConnectionInfo {
        String getUrl();
        String getVersion();
        Map<String, Object> getProperties();
    }
    
    /**
     * 表信息
     */
    interface TableInfo {
        String getName();
        String getType();
        String getComment();
    }
    
    /**
     * 表结构
     */
    interface TableSchema {
        String getTableName();
        List<ColumnInfo> getColumns();
        List<IndexInfo> getIndexes();
    }
    
    /**
     * 列信息
     */
    interface ColumnInfo {
        String getName();
        String getType();
        boolean isNullable();
        String getComment();
        Object getDefaultValue();
    }
    
    /**
     * 索引信息
     */
    interface IndexInfo {
        String getName();
        String getType();
        List<String> getColumns();
        boolean isUnique();
    }
}