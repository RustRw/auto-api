package org.duqiu.fly.autoapi.datasource.exception;

/**
 * 数据源异常基类
 */
public class DataSourceException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] args;
    
    public DataSourceException(String message) {
        super(message);
        this.errorCode = null;
        this.args = null;
    }
    
    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.args = null;
    }
    
    public DataSourceException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public DataSourceException(String errorCode, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public Object[] getArgs() {
        return args;
    }
}