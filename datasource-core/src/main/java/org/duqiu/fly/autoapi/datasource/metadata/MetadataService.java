package org.duqiu.fly.autoapi.datasource.metadata;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.core.DatabaseAwareConnection;
import org.duqiu.fly.autoapi.datasource.core.SchemaAwareConnection;
import org.duqiu.fly.autoapi.datasource.enums.DataSourceType;
import org.duqiu.fly.autoapi.datasource.factory.UnifiedDataSourceFactory;
import org.duqiu.fly.autoapi.datasource.model.DataSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据服务 - 统一管理不同数据源的元数据查询
 */
@Service
public class MetadataService {
    
    private final UnifiedDataSourceFactory dataSourceFactory;
    private final Map<DataSourceType, MetadataAdapter> adapters;
    
    public MetadataService(UnifiedDataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
        this.adapters = new HashMap<>();
        initializeAdapters();
    }
    
    /**
     * 初始化不同数据源类型的元数据适配器
     */
    private void initializeAdapters() {
        // JDBC 数据库适配器
        adapters.put(DataSourceType.MYSQL, new JdbcMetadataAdapter());
        adapters.put(DataSourceType.POSTGRESQL, new JdbcMetadataAdapter());
        adapters.put(DataSourceType.ORACLE, new JdbcMetadataAdapter());
        adapters.put(DataSourceType.CLICKHOUSE, new ClickHouseMetadataAdapter());
        adapters.put(DataSourceType.STARROCKS, new StarRocksMetadataAdapter());
        adapters.put(DataSourceType.TDENGINE, new TDengineMetadataAdapter());
        
        // NoSQL 数据库适配器
        adapters.put(DataSourceType.MONGODB, new MongoMetadataAdapter());
        adapters.put(DataSourceType.ELASTICSEARCH, new ElasticsearchMetadataAdapter());
        adapters.put(DataSourceType.NEBULA_GRAPH, new NebulaGraphMetadataAdapter());
        
        // 其他类型适配器
        adapters.put(DataSourceType.KAFKA, new KafkaMetadataAdapter());
        adapters.put(DataSourceType.HTTP_API, new HttpApiMetadataAdapter());
    }
    
    /**
     * 获取数据源连接信息
     */
    public DataSourceConnection.ConnectionInfo getConnectionInfo(DataSource dataSource) {
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            return connection.getConnectionInfo();
        } catch (Exception e) {
            throw new RuntimeException("获取连接信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取数据库列表
     */
    public List<String> getDatabases(DataSource dataSource) {
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            if (connection instanceof DatabaseAwareConnection) {
                return ((DatabaseAwareConnection) connection).getDatabases();
            }
            
            // 使用适配器获取
            MetadataAdapter adapter = adapters.get(dataSource.getType());
            if (adapter != null) {
                return adapter.getDatabases(connection);
            }
            
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("获取数据库列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取模式列表
     */
    public List<String> getSchemas(DataSource dataSource, String database) {
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            if (connection instanceof SchemaAwareConnection) {
                return ((SchemaAwareConnection) connection).getSchemas();
            }
            
            // 使用适配器获取
            MetadataAdapter adapter = adapters.get(dataSource.getType());
            if (adapter != null) {
                return adapter.getSchemas(connection, database);
            }
            
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("获取模式列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表列表
     */
    public List<DataSourceConnection.TableInfo> getTables(DataSource dataSource, String database, String schema) {
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            if (connection instanceof SchemaAwareConnection && (database != null || schema != null)) {
                return ((SchemaAwareConnection) connection).getTables(database, schema);
            }
            
            // 使用适配器获取
            MetadataAdapter adapter = adapters.get(dataSource.getType());
            if (adapter != null) {
                return adapter.getTables(connection, database, schema);
            }
            
            return connection.getTables();
        } catch (Exception e) {
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表结构
     */
    public DataSourceConnection.TableSchema getTableSchema(DataSource dataSource, String tableName, String database, String schema) {
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            if (connection instanceof SchemaAwareConnection && (database != null || schema != null)) {
                return ((SchemaAwareConnection) connection).getTableSchema(tableName, database, schema);
            }
            
            // 使用适配器获取
            MetadataAdapter adapter = adapters.get(dataSource.getType());
            if (adapter != null) {
                return adapter.getTableSchema(connection, tableName, database, schema);
            }
            
            return connection.getTableSchema(tableName);
        } catch (Exception e) {
            throw new RuntimeException("获取表结构失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表的详细统计信息
     */
    public Map<String, Object> getTableStatistics(DataSource dataSource, String tableName, String database, String schema) {
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            MetadataAdapter adapter = adapters.get(dataSource.getType());
            if (adapter != null) {
                return adapter.getTableStatistics(connection, tableName, database, schema);
            }
            
            return Map.of();
        } catch (Exception e) {
            throw new RuntimeException("获取表统计信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取表的样本数据
     */
    public List<Map<String, Object>> getTableSampleData(DataSource dataSource, String tableName, String database, String schema, int limit) {
        try (DataSourceConnection connection = dataSourceFactory.createConnection(dataSource)) {
            MetadataAdapter adapter = adapters.get(dataSource.getType());
            if (adapter != null) {
                return adapter.getTableSampleData(connection, tableName, database, schema, limit);
            }
            
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("获取表样本数据失败: " + e.getMessage(), e);
        }
    }
}