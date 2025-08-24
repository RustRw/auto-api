package org.duqiu.fly.autoapi.datasource.exception;

/**
 * 数据源相关异常定义
 */
public class DataSourceExceptions {
    
    /**
     * 数据源不存在
     */
    public static class DataSourceNotFoundException extends DataSourceException {
        public DataSourceNotFoundException(Long dataSourceId) {
            super("DATASOURCE_NOT_FOUND", "数据源不存在", dataSourceId);
        }
    }
    
    /**
     * 数据源连接失败
     */
    public static class DataSourceConnectionException extends DataSourceException {
        public DataSourceConnectionException(String message) {
            super("DATASOURCE_CONNECTION_FAILED", message);
        }
        
        public DataSourceConnectionException(String message, Throwable cause) {
            super("DATASOURCE_CONNECTION_FAILED", message, cause);
        }
    }
    
    /**
     * 数据源配置无效
     */
    public static class InvalidDataSourceConfigException extends DataSourceException {
        public InvalidDataSourceConfigException(String message) {
            super("INVALID_DATASOURCE_CONFIG", message);
        }
    }
    
    /**
     * 不支持的数据源类型
     */
    public static class UnsupportedDataSourceTypeException extends DataSourceException {
        public UnsupportedDataSourceTypeException(String dataSourceType) {
            super("UNSUPPORTED_DATASOURCE_TYPE", "不支持的数据源类型: " + dataSourceType, dataSourceType);
        }
    }
    
    /**
     * 依赖不可用
     */
    public static class DependencyNotAvailableException extends DataSourceException {
        public DependencyNotAvailableException(String dependency) {
            super("DEPENDENCY_NOT_AVAILABLE", "依赖不可用: " + dependency, dependency);
        }
    }
    
    /**
     * 查询执行失败
     */
    public static class QueryExecutionException extends DataSourceException {
        public QueryExecutionException(String message) {
            super("QUERY_EXECUTION_FAILED", message);
        }
        
        public QueryExecutionException(String message, Throwable cause) {
            super("QUERY_EXECUTION_FAILED", message, cause);
        }
    }
    
    /**
     * 元数据查询失败
     */
    public static class MetadataQueryException extends DataSourceException {
        public MetadataQueryException(String message) {
            super("METADATA_QUERY_FAILED", message);
        }
        
        public MetadataQueryException(String message, Throwable cause) {
            super("METADATA_QUERY_FAILED", message, cause);
        }
    }
    
    /**
     * 权限不足
     */
    public static class InsufficientPermissionException extends DataSourceException {
        public InsufficientPermissionException(String operation) {
            super("INSUFFICIENT_PERMISSION", "权限不足，无法执行操作: " + operation, operation);
        }
    }
    
    /**
     * 数据源名称重复
     */
    public static class DuplicateDataSourceNameException extends DataSourceException {
        public DuplicateDataSourceNameException(String name) {
            super("DUPLICATE_DATASOURCE_NAME", "数据源名称已存在: " + name, name);
        }
    }
    
    /**
     * 表不存在
     */
    public static class TableNotFoundException extends DataSourceException {
        public TableNotFoundException(String tableName) {
            super("TABLE_NOT_FOUND", "表不存在: " + tableName, tableName);
        }
    }
}