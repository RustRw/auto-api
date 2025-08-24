package org.duqiu.fly.autoapi.datasource.validation;

import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceUpdateRequest;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.exception.DataSourceExceptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 数据源验证器
 */
@Component
public class DataSourceValidator {
    
    private static final Pattern HOST_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|" +
            "^(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)$"
    );
    
    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,63}$");
    
    /**
     * 验证数据源创建请求
     */
    public ValidationResult validateCreateRequest(DataSourceCreateRequestV2 request) {
        List<String> errors = new ArrayList<>();
        
        // 基本字段验证
        validateBasicFields(request.getName(), request.getType(), request.getHost(), 
                           request.getPort(), request.getDatabase(), errors);
        
        // 连接池配置验证
        validateConnectionPoolConfig(request.getMaxPoolSize(), request.getMinPoolSize(),
                                   request.getConnectionTimeout() != null ? request.getConnectionTimeout().intValue() : null, 
                                   request.getIdleTimeout() != null ? request.getIdleTimeout().intValue() : null,
                                   request.getMaxLifetime() != null ? request.getMaxLifetime().intValue() : null, errors);
        
        // 数据源类型特定验证
        validateTypeSpecificConfig(request, errors);
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * 验证数据源更新请求
     */
    public ValidationResult validateUpdateRequest(DataSourceUpdateRequest request) {
        List<String> errors = new ArrayList<>();
        
        // 只验证非空字段
        if (request.getName() != null) {
            validateName(request.getName(), errors);
        }
        
        if (request.getHost() != null) {
            validateHost(request.getHost(), errors);
        }
        
        if (request.getPort() != null) {
            validatePort(request.getPort(), errors);
        }
        
        if (request.getDatabase() != null) {
            validateDatabase(request.getDatabase(), errors);
        }
        
        // 连接池配置验证
        if (request.getMaxPoolSize() != null || request.getMinPoolSize() != null) {
            Integer maxPoolSize = request.getMaxPoolSize() != null ? request.getMaxPoolSize() : 10;
            Integer minPoolSize = request.getMinPoolSize() != null ? request.getMinPoolSize() : 1;
            
            if (minPoolSize > maxPoolSize) {
                errors.add("最小连接数不能大于最大连接数");
            }
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * 验证查询语句
     */
    public ValidationResult validateQuery(String query, DataSourceType dataSourceType) {
        List<String> errors = new ArrayList<>();
        
        if (query == null || query.trim().isEmpty()) {
            errors.add("查询语句不能为空");
            return new ValidationResult(false, errors);
        }
        
        // SQL注入基础检查
        if (containsSqlInjectionPatterns(query)) {
            errors.add("查询语句包含潜在的安全风险");
        }
        
        // 数据源类型特定验证
        validateQueryForDataSourceType(query, dataSourceType, errors);
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
    
    private void validateBasicFields(String name, DataSourceType type, String host, 
                                   Integer port, String database, List<String> errors) {
        validateName(name, errors);
        validateType(type, errors);
        validateHost(host, errors);
        validatePort(port, errors);
        validateDatabase(database, errors);
    }
    
    private void validateName(String name, List<String> errors) {
        if (name == null || name.trim().isEmpty()) {
            errors.add("数据源名称不能为空");
        } else if (name.length() > 100) {
            errors.add("数据源名称长度不能超过100个字符");
        }
    }
    
    private void validateType(DataSourceType type, List<String> errors) {
        if (type == null) {
            errors.add("数据源类型不能为空");
        }
    }
    
    private void validateHost(String host, List<String> errors) {
        if (host == null || host.trim().isEmpty()) {
            errors.add("主机地址不能为空");
        } else if (!HOST_PATTERN.matcher(host).matches()) {
            errors.add("主机地址格式无效");
        }
    }
    
    private void validatePort(Integer port, List<String> errors) {
        if (port == null) {
            errors.add("端口号不能为空");
        } else if (port < 1 || port > 65535) {
            errors.add("端口号必须在1-65535之间");
        }
    }
    
    private void validateDatabase(String database, List<String> errors) {
        if (database != null && !database.trim().isEmpty()) {
            if (!DATABASE_NAME_PATTERN.matcher(database).matches()) {
                errors.add("数据库名称格式无效");
            }
        }
    }
    
    private void validateConnectionPoolConfig(Integer maxPoolSize, Integer minPoolSize,
                                            Integer connectionTimeout, Integer idleTimeout,
                                            Integer maxLifetime, List<String> errors) {
        if (maxPoolSize != null && (maxPoolSize < 1 || maxPoolSize > 100)) {
            errors.add("最大连接数必须在1-100之间");
        }
        
        if (minPoolSize != null && minPoolSize < 0) {
            errors.add("最小连接数不能小于0");
        }
        
        if (maxPoolSize != null && minPoolSize != null && minPoolSize > maxPoolSize) {
            errors.add("最小连接数不能大于最大连接数");
        }
        
        if (connectionTimeout != null && connectionTimeout < 1000) {
            errors.add("连接超时时间不能小于1000毫秒");
        }
        
        if (idleTimeout != null && idleTimeout < 60000) {
            errors.add("空闲超时时间不能小于60秒");
        }
        
        if (maxLifetime != null && maxLifetime < 600000) {
            errors.add("最大生命周期不能小于10分钟");
        }
    }
    
    private void validateTypeSpecificConfig(DataSourceCreateRequestV2 request, List<String> errors) {
        DataSourceType type = request.getType();
        
        if (type == null) {
            return; // Type validation will be handled in validateBasicFields
        }
        
        switch (type.getProtocol()) {
            case JDBC:
                validateJdbcConfig(request, errors);
                break;
            case HTTP:
                validateHttpConfig(request, errors);
                break;
            case NATIVE:
                validateNativeConfig(request, errors);
                break;
        }
    }
    
    private void validateJdbcConfig(DataSourceCreateRequestV2 request, List<String> errors) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            errors.add("JDBC数据源必须提供用户名");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            errors.add("JDBC数据源必须提供密码");
        }
    }
    
    private void validateHttpConfig(DataSourceCreateRequestV2 request, List<String> errors) {
        // HTTP特定验证逻辑
        if (request.getPort() != null && (request.getPort() == 80 || request.getPort() == 443)) {
            // HTTP/HTTPS默认端口验证通过
        }
    }
    
    private void validateNativeConfig(DataSourceCreateRequestV2 request, List<String> errors) {
        // Native协议特定验证逻辑
    }
    
    private boolean containsSqlInjectionPatterns(String query) {
        String upperQuery = query.toUpperCase();
        
        // 简单的SQL注入模式检查
        String[] dangerousPatterns = {
                "DROP TABLE", "DELETE FROM", "TRUNCATE", "ALTER TABLE",
                "CREATE TABLE", "INSERT INTO", "UPDATE ", "EXEC",
                "EXECUTE", "SP_", "XP_"
        };
        
        for (String pattern : dangerousPatterns) {
            if (upperQuery.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void validateQueryForDataSourceType(String query, DataSourceType dataSourceType, List<String> errors) {
        switch (dataSourceType) {
            case MYSQL:
            case POSTGRESQL:
            case ORACLE:
                validateSqlQuery(query, errors);
                break;
            case MONGODB:
                validateMongoQuery(query, errors);
                break;
            case ELASTICSEARCH:
                validateEsQuery(query, errors);
                break;
            default:
                // 其他类型的查询验证
                break;
        }
    }
    
    private void validateSqlQuery(String query, List<String> errors) {
        // SQL查询验证
        if (!query.trim().toUpperCase().startsWith("SELECT")) {
            errors.add("只支持SELECT查询");
        }
    }
    
    private void validateMongoQuery(String query, List<String> errors) {
        // MongoDB查询验证
        if (query.contains("drop") || query.contains("remove")) {
            errors.add("不支持删除操作");
        }
    }
    
    private void validateEsQuery(String query, List<String> errors) {
        // Elasticsearch查询验证
        if (!query.startsWith("GET") && !query.startsWith("POST")) {
            errors.add("只支持GET和POST查询");
        }
    }
    
    /**
     * 验证结果
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
}