package org.duqiu.fly.autoapi.datasource.metadata;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;
import org.duqiu.fly.autoapi.datasource.jdbc.JdbcConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * JDBC数据源元数据适配器
 */
public class JdbcMetadataAdapter implements MetadataAdapter {
    
    @Override
    public List<String> getDatabases(DataSourceConnection connection) {
        if (!(connection instanceof JdbcConnection)) {
            return List.of();
        }
        
        List<String> databases = new ArrayList<>();
        try {
            Connection jdbcConn = ((JdbcConnection) connection).getJdbcConnection();
            DatabaseMetaData metaData = jdbcConn.getMetaData();
            
            try (ResultSet rs = metaData.getCatalogs()) {
                while (rs.next()) {
                    databases.add(rs.getString("TABLE_CAT"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取数据库列表失败", e);
        }
        
        return databases;
    }
    
    @Override
    public List<String> getSchemas(DataSourceConnection connection, String database) {
        if (!(connection instanceof JdbcConnection)) {
            return List.of();
        }
        
        List<String> schemas = new ArrayList<>();
        try {
            Connection jdbcConn = ((JdbcConnection) connection).getJdbcConnection();
            DatabaseMetaData metaData = jdbcConn.getMetaData();
            
            try (ResultSet rs = metaData.getSchemas(database, null)) {
                while (rs.next()) {
                    schemas.add(rs.getString("TABLE_SCHEM"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取模式列表失败", e);
        }
        
        return schemas;
    }
    
    @Override
    public List<DataSourceConnection.TableInfo> getTables(DataSourceConnection connection, String database, String schema) {
        if (!(connection instanceof JdbcConnection)) {
            return connection.getTables();
        }
        
        List<DataSourceConnection.TableInfo> tables = new ArrayList<>();
        try {
            Connection jdbcConn = ((JdbcConnection) connection).getJdbcConnection();
            DatabaseMetaData metaData = jdbcConn.getMetaData();
            
            try (ResultSet rs = metaData.getTables(database, schema, null, new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    tables.add(new JdbcTableInfo(
                            rs.getString("TABLE_NAME"),
                            rs.getString("TABLE_TYPE"),
                            rs.getString("REMARKS")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("获取表列表失败", e);
        }
        
        return tables;
    }
    
    @Override
    public DataSourceConnection.TableSchema getTableSchema(DataSourceConnection connection, String tableName, String database, String schema) {
        if (!(connection instanceof JdbcConnection)) {
            return connection.getTableSchema(tableName);
        }
        
        try {
            Connection jdbcConn = ((JdbcConnection) connection).getJdbcConnection();
            DatabaseMetaData metaData = jdbcConn.getMetaData();
            
            List<DataSourceConnection.ColumnInfo> columns = new ArrayList<>();
            List<DataSourceConnection.IndexInfo> indexes = new ArrayList<>();
            
            // 获取列信息
            try (ResultSet rs = metaData.getColumns(database, schema, tableName, null)) {
                while (rs.next()) {
                    columns.add(new JdbcColumnInfo(
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable,
                            rs.getString("REMARKS"),
                            rs.getString("COLUMN_DEF")
                    ));
                }
            }
            
            // 获取索引信息
            try (ResultSet rs = metaData.getIndexInfo(database, schema, tableName, false, false)) {
                Map<String, JdbcIndexBuilder> indexBuilders = new HashMap<>();
                
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexName != null) {
                        try {
                            JdbcIndexBuilder builder = indexBuilders.computeIfAbsent(indexName, 
                                    k -> {
                                        try {
                                            return new JdbcIndexBuilder(indexName, !rs.getBoolean("NON_UNIQUE"));
                                        } catch (SQLException e) {
                                            return new JdbcIndexBuilder(indexName, false);
                                        }
                                    });
                            builder.addColumn(rs.getString("COLUMN_NAME"));
                        } catch (SQLException e) {
                            // 忽略该索引
                        }
                    }
                }
                
                indexBuilders.values().forEach(builder -> indexes.add(builder.build()));
            }
            
            return new JdbcTableSchema(tableName, columns, indexes);
            
        } catch (SQLException e) {
            throw new RuntimeException("获取表结构失败", e);
        }
    }
    
    @Override
    public Map<String, Object> getTableStatistics(DataSourceConnection connection, String tableName, String database, String schema) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取表行数
            String countQuery = String.format("SELECT COUNT(*) as row_count FROM %s", 
                    buildFullTableName(tableName, database, schema));
            DataSourceConnection.QueryResult result = connection.executeQuery(countQuery, Map.of());
            
            if (result.isSuccess() && !result.getData().isEmpty()) {
                Object rowCount = result.getData().get(0).get("row_count");
                stats.put("rowCount", rowCount);
            }
            
            stats.put("tableName", tableName);
            stats.put("database", database);
            stats.put("schema", schema);
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    @Override
    public List<Map<String, Object>> getTableSampleData(DataSourceConnection connection, String tableName, String database, String schema, int limit) {
        try {
            String sampleQuery = String.format("SELECT * FROM %s LIMIT %d", 
                    buildFullTableName(tableName, database, schema), limit);
            DataSourceConnection.QueryResult result = connection.executeQuery(sampleQuery, Map.of());
            
            if (result.isSuccess()) {
                return result.getData();
            }
            
            return List.of();
        } catch (Exception e) {
            throw new RuntimeException("获取样本数据失败", e);
        }
    }
    
    @Override
    public boolean tableExists(DataSourceConnection connection, String tableName, String database, String schema) {
        if (!(connection instanceof JdbcConnection)) {
            return false;
        }
        
        try {
            Connection jdbcConn = ((JdbcConnection) connection).getJdbcConnection();
            DatabaseMetaData metaData = jdbcConn.getMetaData();
            
            try (ResultSet rs = metaData.getTables(database, schema, tableName, new String[]{"TABLE", "VIEW"})) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
    
    private String buildFullTableName(String tableName, String database, String schema) {
        StringBuilder sb = new StringBuilder();
        
        if (database != null && !database.isEmpty()) {
            sb.append(database).append(".");
        }
        
        if (schema != null && !schema.isEmpty()) {
            sb.append(schema).append(".");
        }
        
        sb.append(tableName);
        return sb.toString();
    }
    
    // 内部实现类
    private static class JdbcTableInfo implements DataSourceConnection.TableInfo {
        private final String name;
        private final String type;
        private final String comment;
        
        public JdbcTableInfo(String name, String type, String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getType() {
            return type;
        }
        
        @Override
        public String getComment() {
            return comment;
        }
    }
    
    private static class JdbcColumnInfo implements DataSourceConnection.ColumnInfo {
        private final String name;
        private final String type;
        private final boolean nullable;
        private final String comment;
        private final Object defaultValue;
        
        public JdbcColumnInfo(String name, String type, boolean nullable, String comment, Object defaultValue) {
            this.name = name;
            this.type = type;
            this.nullable = nullable;
            this.comment = comment;
            this.defaultValue = defaultValue;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getType() {
            return type;
        }
        
        @Override
        public boolean isNullable() {
            return nullable;
        }
        
        @Override
        public String getComment() {
            return comment;
        }
        
        @Override
        public Object getDefaultValue() {
            return defaultValue;
        }
    }
    
    private static class JdbcIndexInfo implements DataSourceConnection.IndexInfo {
        private final String name;
        private final String type;
        private final List<String> columns;
        private final boolean unique;
        
        public JdbcIndexInfo(String name, String type, List<String> columns, boolean unique) {
            this.name = name;
            this.type = type;
            this.columns = columns;
            this.unique = unique;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getType() {
            return type;
        }
        
        @Override
        public List<String> getColumns() {
            return columns;
        }
        
        @Override
        public boolean isUnique() {
            return unique;
        }
    }
    
    private static class JdbcTableSchema implements DataSourceConnection.TableSchema {
        private final String tableName;
        private final List<DataSourceConnection.ColumnInfo> columns;
        private final List<DataSourceConnection.IndexInfo> indexes;
        
        public JdbcTableSchema(String tableName, List<DataSourceConnection.ColumnInfo> columns, List<DataSourceConnection.IndexInfo> indexes) {
            this.tableName = tableName;
            this.columns = columns;
            this.indexes = indexes;
        }
        
        @Override
        public String getTableName() {
            return tableName;
        }
        
        @Override
        public List<DataSourceConnection.ColumnInfo> getColumns() {
            return columns;
        }
        
        @Override
        public List<DataSourceConnection.IndexInfo> getIndexes() {
            return indexes;
        }
    }
    
    private static class JdbcIndexBuilder {
        private final String name;
        private final boolean unique;
        private final List<String> columns = new ArrayList<>();
        
        public JdbcIndexBuilder(String name, boolean unique) {
            this.name = name;
            this.unique = unique;
        }
        
        public void addColumn(String column) {
            if (column != null) {
                columns.add(column);
            }
        }
        
        public JdbcIndexInfo build() {
            return new JdbcIndexInfo(name, unique ? "UNIQUE" : "INDEX", columns, unique);
        }
    }
}