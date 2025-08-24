package org.duqiu.fly.autoapi.datasource.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceCreateRequestV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceResponseV2;
import org.duqiu.fly.autoapi.datasource.dto.DataSourceUpdateRequest;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.duqiu.fly.autoapi.datasource.repository.DataSourceRepository;
import org.duqiu.fly.autoapi.datasource.core.DatabaseAwareConnection;
import org.duqiu.fly.autoapi.datasource.core.SchemaAwareConnection;
import org.duqiu.fly.autoapi.datasource.core.QueryValidationCapable;
import org.duqiu.fly.autoapi.common.dto.PageResult;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 增强的数据源服务 - 支持多种数据源类型
 */
@Service
public class EnhancedDataSourceService {
    
    private final DataSourceRepository dataSourceRepository;
    private final UnifiedDataSourceFactory dataSourceFactory;
    private final ObjectMapper objectMapper;
    
    public EnhancedDataSourceService(DataSourceRepository dataSourceRepository,
                                   UnifiedDataSourceFactory dataSourceFactory,
                                   ObjectMapper objectMapper) {
        this.dataSourceRepository = dataSourceRepository;
        this.dataSourceFactory = dataSourceFactory;
        this.objectMapper = objectMapper;
    }
    
    public DataSourceResponseV2 createDataSource(DataSourceCreateRequestV2 request, Long userId, Long tenantId) {
        // 验证配置
        if (!request.isValid()) {
            throw new RuntimeException("数据源配置无效");
        }
        
        // 检查名称是否重复
        if (dataSourceRepository.existsByNameAndTenantId(request.getName(), tenantId)) {
            throw new RuntimeException("数据源名称已存在");
        }
        
        // 验证数据源配置
        DataSource tempDataSource = buildDataSourceFromRequest(request, userId);
        var validationResult = dataSourceFactory.validateConfiguration(tempDataSource);
        if (!validationResult.isValid()) {
            throw new RuntimeException("数据源配置验证失败: " + validationResult.getErrorMessage());
        }
        
        // 测试连接
        if (!dataSourceFactory.testConnection(tempDataSource)) {
            throw new RuntimeException("无法连接到数据源，请检查配置");
        }
        
        // 保存数据源
        DataSource dataSource = buildDataSourceFromRequest(request, userId);
        dataSource.setConnectionUrl(dataSourceFactory.buildConnectionUrl(dataSource));
        dataSource.setTestQuery(request.getDefaultTestQuery());
        
        if (request.getAdditionalProperties() != null) {
            try {
                dataSource.setAdditionalProperties(objectMapper.writeValueAsString(request.getAdditionalProperties()));
            } catch (JsonProcessingException e) {
                // 日志记录错误，但不阻止创建
            }
        }
        
        DataSource saved = dataSourceRepository.save(dataSource);
        return convertToResponseV2(saved);
    }
    
    public List<DataSourceResponseV2> getTenantDataSources(Long tenantId) {
        List<DataSource> dataSources = dataSourceRepository.findByTenantIdAndEnabledTrue(tenantId);
        return dataSources.stream()
                .map(this::convertToResponseV2)
                .collect(Collectors.toList());
    }
    
    public PageResult<DataSourceResponseV2> getUserDataSources(Long tenantId, Pageable pageable, DataSourceType type, String keyword) {
        var page = dataSourceRepository.findByTenantIdAndFilters(tenantId, type, keyword, pageable);
        var responses = page.getContent().stream()
                .map(this::convertToResponseV2)
                .collect(Collectors.toList());
        
        return PageResult.of(page.map(this::convertToResponseV2));
    }
    
    public DataSourceResponseV2 getDataSourceById(Long id, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        return convertToResponseV2(dataSource);
    }
    
    public DataSourceResponseV2.ConnectionStatus testConnectionDetailed(Long id, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        DataSourceResponseV2.ConnectionStatus status = new DataSourceResponseV2.ConnectionStatus();
        status.setLastTestTime(LocalDateTime.now());
        
        long startTime = System.currentTimeMillis();
        try {
            boolean connected = dataSourceFactory.testConnection(dataSource);
            long responseTime = System.currentTimeMillis() - startTime;
            
            status.setConnected(connected);
            status.setResponseTime(responseTime);
            status.setMessage(connected ? "连接成功" : "连接失败");
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            status.setConnected(false);
            status.setResponseTime(responseTime);
            status.setMessage("连接失败: " + e.getMessage());
        }
        
        return status;
    }
    
    public List<DataSourceConnection.TableInfo> getDataSourceTables(Long id, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            return connection.getTables();
        } catch (Exception e) {
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }
    }
    
    public DataSourceConnection.TableSchema getTableSchema(Long id, String tableName, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            return connection.getTableSchema(tableName);
        } catch (Exception e) {
            throw new RuntimeException("获取表结构失败: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> executeQuery(Long id, String query, Map<String, Object> parameters, Long userId) {
        return executeQuery(id, query, parameters, 100, userId);
    }
    
    public Map<String, Object> executeQuery(Long id, String query, Map<String, Object> parameters, Integer limit, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            DataSourceConnection.QueryResult result = connection.executeQuery(query, parameters);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("data", result.getData());
            response.put("count", result.getCount());
            response.put("columns", result.getColumns());
            response.put("executionTime", result.getExecutionTime());
            if (!result.isSuccess()) {
                response.put("errorMessage", result.getErrorMessage());
            }
            
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorMessage", e.getMessage());
            return response;
        }
    }
    
    // 元数据查询方法
    public DataSourceConnection.ConnectionInfo getDataSourceMetadata(Long id, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            return connection.getConnectionInfo();
        } catch (Exception e) {
            throw new RuntimeException("获取数据源元数据失败: " + e.getMessage(), e);
        }
    }
    
    public List<String> getDatabases(Long id, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            if (connection instanceof DatabaseAwareConnection) {
                return ((DatabaseAwareConnection) connection).getDatabases();
            } else {
                // 如果不支持数据库列表查询，返回空列表
                return List.of();
            }
        } catch (Exception e) {
            throw new RuntimeException("获取数据库列表失败: " + e.getMessage(), e);
        }
    }
    
    public List<DataSourceConnection.TableInfo> getTables(Long id, String database, String schema, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            if (connection instanceof SchemaAwareConnection) {
                return ((SchemaAwareConnection) connection).getTables(database, schema);
            } else {
                return connection.getTables();
            }
        } catch (Exception e) {
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }
    }
    
    public DataSourceConnection.TableSchema getTableSchema(Long id, String tableName, String database, String schema, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            if (connection instanceof SchemaAwareConnection) {
                return ((SchemaAwareConnection) connection).getTableSchema(tableName, database, schema);
            } else {
                return connection.getTableSchema(tableName);
            }
        } catch (Exception e) {
            throw new RuntimeException("获取表结构失败: " + e.getMessage(), e);
        }
    }
    
    public List<DataSourceConnection.ColumnInfo> getTableColumns(Long id, String tableName, String database, String schema, Long userId) {
        DataSourceConnection.TableSchema tableSchema = getTableSchema(id, tableName, database, schema, userId);
        return tableSchema.getColumns();
    }
    
    public List<DataSourceConnection.IndexInfo> getTableIndexes(Long id, String tableName, String database, String schema, Long userId) {
        DataSourceConnection.TableSchema tableSchema = getTableSchema(id, tableName, database, schema, userId);
        return tableSchema.getIndexes();
    }
    
    public Map<String, Object> validateQuery(Long id, String query, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        Map<String, Object> result = new HashMap<>();
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            if (connection instanceof QueryValidationCapable) {
                QueryValidationCapable validator = (QueryValidationCapable) connection;
                boolean valid = validator.validateQuery(query);
                result.put("valid", valid);
                if (valid) {
                    result.put("message", "查询语句验证通过");
                } else {
                    result.put("message", "查询语句验证失败");
                }
            } else {
                result.put("valid", true);
                result.put("message", "该数据源不支持查询验证");
            }
        } catch (Exception e) {
            result.put("valid", false);
            result.put("message", "验证失败: " + e.getMessage());
        }
        
        return result;
    }
    
    public List<DataSourceType> getSupportedDataSourceTypes() {
        return List.of(DataSourceType.values());
    }
    
    public UnifiedDataSourceFactory.DependencyInfo getDependencyInfo(DataSourceType type) {
        return dataSourceFactory.getDependencyInfo(type);
    }
    
    public Map<DataSourceType, Boolean> checkAllDependencies() {
        Map<DataSourceType, Boolean> statusMap = new HashMap<>();
        for (DataSourceType type : DataSourceType.values()) {
            statusMap.put(type, dataSourceFactory.isDependencyAvailable(type));
        }
        return statusMap;
    }
    
    public void deleteDataSource(Long id, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权删除该数据源");
        }
        
        dataSource.setEnabled(false);
        dataSource.setUpdatedBy(userId);
        dataSourceRepository.save(dataSource);
    }
    
    public DataSourceResponseV2 updateDataSource(Long id, DataSourceUpdateRequest request, Long userId) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权更新该数据源");
        }
        
        // 更新字段
        if (request.getName() != null) {
            dataSource.setName(request.getName());
        }
        if (request.getDescription() != null) {
            dataSource.setDescription(request.getDescription());
        }
        if (request.getHost() != null) {
            dataSource.setHost(request.getHost());
        }
        if (request.getPort() != null) {
            dataSource.setPort(request.getPort());
        }
        if (request.getDatabase() != null) {
            dataSource.setDatabase(request.getDatabase());
        }
        if (request.getUsername() != null) {
            dataSource.setUsername(request.getUsername());
        }
        if (request.getPassword() != null) {
            dataSource.setPassword(request.getPassword());
        }
        if (request.getVersion() != null) {
            dataSource.setVersion(request.getVersion());
        }
        if (request.getMaxPoolSize() != null) {
            dataSource.setMaxPoolSize(request.getMaxPoolSize());
        }
        if (request.getMinPoolSize() != null) {
            dataSource.setMinPoolSize(request.getMinPoolSize());
        }
        if (request.getConnectionTimeout() != null) {
            dataSource.setConnectionTimeout(request.getConnectionTimeout().intValue());
        }
        if (request.getIdleTimeout() != null) {
            dataSource.setIdleTimeout(request.getIdleTimeout().intValue());
        }
        if (request.getMaxLifetime() != null) {
            dataSource.setMaxLifetime(request.getMaxLifetime().intValue());
        }
        if (request.getSslEnabled() != null) {
            dataSource.setSslEnabled(request.getSslEnabled());
        }
        if (request.getConnectionPoolEnabled() != null) {
            dataSource.setConnectionPoolEnabled(request.getConnectionPoolEnabled());
        }
        if (request.getEnabled() != null) {
            dataSource.setEnabled(request.getEnabled());
        }
        
        dataSource.setUpdatedBy(userId);
        
        // 处理额外属性
        if (request.getAdditionalProperties() != null) {
            try {
                dataSource.setAdditionalProperties(objectMapper.writeValueAsString(request.getAdditionalProperties()));
            } catch (JsonProcessingException e) {
                // 日志记录错误，但不阻止更新
            }
        }
        
        // 重新生成连接URL
        dataSource.setConnectionUrl(dataSourceFactory.buildConnectionUrl(dataSource));
        
        DataSource saved = dataSourceRepository.save(dataSource);
        return convertToResponseV2(saved);
    }
    
    public void batchDeleteDataSources(List<Long> ids, Long userId) {
        for (Long id : ids) {
            deleteDataSource(id, userId);
        }
    }
    
    public DataSourceResponseV2.ConnectionStatus testDataSourceConfig(DataSourceCreateRequestV2 request, Long userId) {
        DataSource tempDataSource = buildDataSourceFromRequest(request, userId);
        
        DataSourceResponseV2.ConnectionStatus status = new DataSourceResponseV2.ConnectionStatus();
        status.setLastTestTime(LocalDateTime.now());
        
        long startTime = System.currentTimeMillis();
        try {
            boolean connected = dataSourceFactory.testConnection(tempDataSource);
            long responseTime = System.currentTimeMillis() - startTime;
            
            status.setConnected(connected);
            status.setResponseTime(responseTime);
            status.setMessage(connected ? "连接成功" : "连接失败");
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            status.setConnected(false);
            status.setResponseTime(responseTime);
            status.setMessage("连接失败: " + e.getMessage());
        }
        
        return status;
    }
    
    private DataSource buildDataSourceFromRequest(DataSourceCreateRequestV2 request, Long userId) {
        DataSource dataSource = new DataSource();
        dataSource.setName(request.getName());
        dataSource.setDescription(request.getDescription());
        dataSource.setType(request.getType());
        dataSource.setHost(request.getHost());
        dataSource.setPort(request.getPort());
        dataSource.setDatabase(request.getDatabase());
        dataSource.setUsername(request.getUsername());
        dataSource.setPassword(request.getPassword());
        dataSource.setVersion(request.getVersion());
        dataSource.setMaxPoolSize(request.getMaxPoolSize());
        dataSource.setMinPoolSize(request.getMinPoolSize());
        dataSource.setConnectionTimeout(request.getConnectionTimeout());
        dataSource.setIdleTimeout(request.getIdleTimeout());
        dataSource.setMaxLifetime(request.getMaxLifetime());
        dataSource.setSslEnabled(request.getSslEnabled());
        dataSource.setConnectionPoolEnabled(request.getConnectionPoolEnabled());
        dataSource.setCreatedBy(userId);
        dataSource.setUpdatedBy(userId);
        return dataSource;
    }
    
    private DataSourceResponseV2 convertToResponseV2(DataSource dataSource) {
        DataSourceResponseV2 response = new DataSourceResponseV2();
        response.setId(dataSource.getId());
        response.setName(dataSource.getName());
        response.setDescription(dataSource.getDescription());
        response.setType(dataSource.getType());
        response.setCategory(dataSource.getType().getCategory());
        response.setProtocol(dataSource.getType().getProtocol());
        response.setHost(dataSource.getHost());
        response.setPort(dataSource.getPort());
        response.setDatabase(dataSource.getDatabase());
        response.setUsername(dataSource.getUsername());
        response.setVersion(dataSource.getVersion());
        response.setSupportedVersions(dataSource.getType().getSupportedVersions());
        response.setMaxPoolSize(dataSource.getMaxPoolSize());
        response.setMinPoolSize(dataSource.getMinPoolSize());
        response.setConnectionTimeout(dataSource.getConnectionTimeout());
        response.setIdleTimeout(dataSource.getIdleTimeout());
        response.setMaxLifetime(dataSource.getMaxLifetime());
        response.setEnabled(dataSource.getEnabled());
        response.setSslEnabled(dataSource.getSslEnabled());
        response.setConnectionPoolEnabled(dataSource.getConnectionPoolEnabled());
        response.setTestQuery(dataSource.getTestQuery());
        response.setCreatedAt(dataSource.getCreatedAt());
        response.setUpdatedAt(dataSource.getUpdatedAt());
        
        // 解析额外属性
        if (dataSource.getAdditionalProperties() != null) {
            try {
                response.setAdditionalProperties(objectMapper.readValue(
                    dataSource.getAdditionalProperties(), Map.class));
            } catch (JsonProcessingException e) {
                // 日志记录错误
            }
        }
        
        // 添加依赖信息
        UnifiedDataSourceFactory.DependencyInfo depInfo = dataSourceFactory.getDependencyInfo(dataSource.getType());
        DataSourceResponseV2.DependencyInfo dependencyInfo = new DataSourceResponseV2.DependencyInfo();
        dependencyInfo.setCoordinate(depInfo.getCoordinate());
        dependencyInfo.setAvailable(dataSourceFactory.isDependencyAvailable(dataSource.getType()));
        dependencyInfo.setRecommendedVersion(depInfo.getRecommendedVersion());
        dependencyInfo.setSupportedVersions(depInfo.getSupportedVersions());
        dependencyInfo.setInstallCommand(generateInstallCommand(depInfo.getCoordinate(), depInfo.getRecommendedVersion()));
        response.setDependencyInfo(dependencyInfo);
        
        return response;
    }
    
    private String generateInstallCommand(String coordinate, String version) {
        // 生成Gradle依赖添加命令
        return String.format("implementation '%s:%s'", coordinate, version);
    }
    
    // Legacy compatibility methods for tests
    public DataSourceResponseV2 createDataSource(DataSourceCreateRequestV2 request, Long userId) {
        return createDataSource(request, userId, 1L); // Default tenant
    }
    
    public List<DataSourceResponseV2> getUserDataSources(Long userId) {
        return getTenantDataSources(1L); // Default tenant
    }
    
    // 新增数据查看功能相关方法
    
    /**
     * 获取数据源的数据库列表（支持搜索）
     */
    public List<String> getDatabases(Long id, Long userId, String search) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            List<String> databases;
            if (connection instanceof DatabaseAwareConnection) {
                databases = ((DatabaseAwareConnection) connection).getDatabases();
            } else {
                // 对于不支持多数据库的数据源，返回默认数据库
                databases = dataSource.getDatabase() != null ? 
                    List.of(dataSource.getDatabase()) : List.of("default");
            }
            
            // 如果有搜索条件，进行过滤
            if (search != null && !search.trim().isEmpty()) {
                final String searchLower = search.toLowerCase().trim();
                databases = databases.stream()
                    .filter(db -> db.toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
            }
            
            return databases;
        } catch (Exception e) {
            throw new RuntimeException("获取数据库列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取指定数据库的表列表（支持搜索）
     */
    public List<String> getTables(Long id, Long userId, String database, String search) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            List<DataSourceConnection.TableInfo> tableInfos;
            if (connection instanceof SchemaAwareConnection) {
                tableInfos = ((SchemaAwareConnection) connection).getTables(database, null);
            } else {
                tableInfos = connection.getTables();
            }
            
            // 提取表名
            List<String> tableNames = tableInfos.stream()
                .map(DataSourceConnection.TableInfo::getName)
                .collect(Collectors.toList());
            
            // 如果有搜索条件，进行过滤
            if (search != null && !search.trim().isEmpty()) {
                final String searchLower = search.toLowerCase().trim();
                tableNames = tableNames.stream()
                    .filter(table -> table.toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
            }
            
            return tableNames;
        } catch (Exception e) {
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表结构信息
     */
    public List<Map<String, Object>> getTableStructure(Long id, Long userId, String database, String table) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            DataSourceConnection.TableSchema schema;
            if (connection instanceof SchemaAwareConnection) {
                schema = ((SchemaAwareConnection) connection).getTableSchema(table, database, null);
            } else {
                schema = connection.getTableSchema(table);
            }
            
            return schema.getColumns().stream().map(column -> {
                Map<String, Object> columnInfo = new HashMap<>();
                columnInfo.put("name", column.getName());
                columnInfo.put("type", column.getType());
                columnInfo.put("nullable", column.isNullable());
                columnInfo.put("defaultValue", column.getDefaultValue());
                columnInfo.put("comment", column.getComment());
                
                // 基础接口没有这些属性，我们暂时设为默认值
                columnInfo.put("size", null);
                columnInfo.put("primaryKey", false);
                columnInfo.put("autoIncrement", false);
                
                return columnInfo;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("获取表结构失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表示例数据
     */
    public Map<String, Object> getTableSampleData(Long id, Long userId, String database, String table, int limit) {
        DataSource dataSource = dataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("数据源不存在"));
        
        if (!dataSource.getCreatedBy().equals(userId)) {
            throw new RuntimeException("无权访问该数据源");
        }
        
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            // 构建查询语句
            String query;
            Map<String, Object> parameters = new HashMap<>();
            
            // 根据数据源类型构建不同的查询语句
            switch (dataSource.getType()) {
                case H2:
                case MYSQL:
                case POSTGRESQL:
                    query = String.format("SELECT * FROM %s LIMIT %d", table, limit);
                    break;
                case ORACLE:
                    query = String.format("SELECT * FROM %s WHERE ROWNUM <= %d", table, limit);
                    break;
                default:
                    query = String.format("SELECT * FROM %s LIMIT %d", table, limit);
            }
            
            DataSourceConnection.QueryResult result = connection.executeQuery(query, parameters);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("data", result.getData());
            response.put("columns", result.getColumns());
            response.put("count", result.getCount());
            response.put("executionTime", result.getExecutionTime());
            response.put("tableName", table);
            response.put("database", database);
            
            if (!result.isSuccess()) {
                response.put("errorMessage", result.getErrorMessage());
            }
            
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorMessage", "获取示例数据失败: " + e.getMessage());
            response.put("tableName", table);
            response.put("database", database);
            return response;
        }
    }
}