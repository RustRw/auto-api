package org.duqiu.fly.autoapi.datasource.jdbc;

import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;

import java.sql.*;
import java.util.*;

/**
 * JDBC数据源连接实现
 */
public class JdbcConnection implements DataSourceConnection {
    
    private final Connection connection;
    private final String url;
    private final DatabaseMetaData metaData;
    
    public JdbcConnection(Connection connection) throws SQLException {
        this.connection = connection;
        this.url = connection.getMetaData().getURL();
        this.metaData = connection.getMetaData();
    }
    
    public Connection getJdbcConnection() {
        return connection;
    }
    
    @Override
    public boolean isValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public QueryResult executeQuery(String query, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            
            // 设置参数
            setParameters(stmt, parameters);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<Map<String, Object>> data = new ArrayList<>();
                List<ColumnInfo> columns = extractColumnInfo(rs.getMetaData());
                
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                        String columnName = rs.getMetaData().getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    data.add(row);
                }
                
                long executionTime = System.currentTimeMillis() - startTime;
                return new JdbcQueryResult(data, data.size(), columns, executionTime, true, null);
            }
        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new JdbcQueryResult(Collections.emptyList(), 0, Collections.emptyList(), 
                                     executionTime, false, e.getMessage());
        }
    }
    
    @Override
    public UpdateResult executeUpdate(String command, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        try (PreparedStatement stmt = connection.prepareStatement(command)) {
            
            setParameters(stmt, parameters);
            int affectedRows = stmt.executeUpdate();
            
            long executionTime = System.currentTimeMillis() - startTime;
            return new JdbcUpdateResult(affectedRows, executionTime, true, null);
        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new JdbcUpdateResult(0, executionTime, false, e.getMessage());
        }
    }
    
    @Override
    public ConnectionInfo getConnectionInfo() {
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put("databaseProductName", metaData.getDatabaseProductName());
            properties.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            properties.put("driverName", metaData.getDriverName());
            properties.put("driverVersion", metaData.getDriverVersion());
            properties.put("maxConnections", metaData.getMaxConnections());
            
            return new JdbcConnectionInfo(url, metaData.getDatabaseProductVersion(), properties);
        } catch (SQLException e) {
            return new JdbcConnectionInfo(url, "unknown", Collections.emptyMap());
        }
    }
    
    @Override
    public List<TableInfo> getTables() {
        List<TableInfo> tables = new ArrayList<>();
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String tableType = rs.getString("TABLE_TYPE");
                String remarks = rs.getString("REMARKS");
                tables.add(new JdbcTableInfo(tableName, tableType, remarks));
            }
        } catch (SQLException e) {
            // 日志记录错误，返回空列表
        }
        return tables;
    }
    
    @Override
    public TableSchema getTableSchema(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        List<IndexInfo> indexes = new ArrayList<>();
        
        // 获取列信息
        try (ResultSet rs = metaData.getColumns(null, null, tableName, "%")) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String dataType = rs.getString("TYPE_NAME");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                String remarks = rs.getString("REMARKS");
                String defaultValue = rs.getString("COLUMN_DEF");
                
                columns.add(new JdbcColumnInfo(columnName, dataType, nullable, remarks, defaultValue));
            }
        } catch (SQLException e) {
            // 日志记录错误
        }
        
        // 获取索引信息
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName, false, false)) {
            Map<String, List<String>> indexColumns = new HashMap<>();
            Map<String, Boolean> indexUnique = new HashMap<>();
            
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName == null) continue;
                
                String columnName = rs.getString("COLUMN_NAME");
                boolean unique = !rs.getBoolean("NON_UNIQUE");
                
                indexColumns.computeIfAbsent(indexName, k -> new ArrayList<>()).add(columnName);
                indexUnique.put(indexName, unique);
            }
            
            for (Map.Entry<String, List<String>> entry : indexColumns.entrySet()) {
                String indexName = entry.getKey();
                List<String> cols = entry.getValue();
                boolean unique = indexUnique.get(indexName);
                indexes.add(new JdbcIndexInfo(indexName, "BTREE", cols, unique));
            }
        } catch (SQLException e) {
            // 日志记录错误
        }
        
        return new JdbcTableSchema(tableName, columns, indexes);
    }
    
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            // 日志记录错误
        }
    }
    
    private void setParameters(PreparedStatement stmt, Map<String, Object> parameters) throws SQLException {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        
        // 简化处理：按照参数顺序设置
        int index = 1;
        for (Object value : parameters.values()) {
            stmt.setObject(index++, value);
        }
    }
    
    private List<ColumnInfo> extractColumnInfo(ResultSetMetaData metaData) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String name = metaData.getColumnName(i);
            String type = metaData.getColumnTypeName(i);
            boolean nullable = metaData.isNullable(i) == ResultSetMetaData.columnNullable;
            
            columns.add(new JdbcColumnInfo(name, type, nullable, null, null));
        }
        return columns;
    }
    
    // 内部实现类
    static class JdbcQueryResult implements QueryResult {
        private final List<Map<String, Object>> data;
        private final long count;
        private final List<ColumnInfo> columns;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public JdbcQueryResult(List<Map<String, Object>> data, long count, List<ColumnInfo> columns,
                              long executionTime, boolean success, String errorMessage) {
            this.data = data;
            this.count = count;
            this.columns = columns;
            this.executionTime = executionTime;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        @Override
        public List<Map<String, Object>> getData() { return data; }
        @Override
        public long getCount() { return count; }
        @Override
        public List<ColumnInfo> getColumns() { return columns; }
        @Override
        public long getExecutionTime() { return executionTime; }
        @Override
        public boolean isSuccess() { return success; }
        @Override
        public String getErrorMessage() { return errorMessage; }
    }
    
    static class JdbcUpdateResult implements UpdateResult {
        private final long affectedRows;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public JdbcUpdateResult(long affectedRows, long executionTime, boolean success, String errorMessage) {
            this.affectedRows = affectedRows;
            this.executionTime = executionTime;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        @Override
        public long getAffectedRows() { return affectedRows; }
        @Override
        public long getExecutionTime() { return executionTime; }
        @Override
        public boolean isSuccess() { return success; }
        @Override
        public String getErrorMessage() { return errorMessage; }
    }
    
    static class JdbcConnectionInfo implements ConnectionInfo {
        private final String url;
        private final String version;
        private final Map<String, Object> properties;
        
        public JdbcConnectionInfo(String url, String version, Map<String, Object> properties) {
            this.url = url;
            this.version = version;
            this.properties = properties;
        }
        
        @Override
        public String getUrl() { return url; }
        @Override
        public String getVersion() { return version; }
        @Override
        public Map<String, Object> getProperties() { return properties; }
    }
    
    static class JdbcTableInfo implements TableInfo {
        private final String name;
        private final String type;
        private final String comment;
        
        public JdbcTableInfo(String name, String type, String comment) {
            this.name = name;
            this.type = type;
            this.comment = comment;
        }
        
        @Override
        public String getName() { return name; }
        @Override
        public String getType() { return type; }
        @Override
        public String getComment() { return comment; }
    }
    
    static class JdbcColumnInfo implements ColumnInfo {
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
        public String getName() { return name; }
        @Override
        public String getType() { return type; }
        @Override
        public boolean isNullable() { return nullable; }
        @Override
        public String getComment() { return comment; }
        @Override
        public Object getDefaultValue() { return defaultValue; }
    }
    
    static class JdbcIndexInfo implements IndexInfo {
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
        public String getName() { return name; }
        @Override
        public String getType() { return type; }
        @Override
        public List<String> getColumns() { return columns; }
        @Override
        public boolean isUnique() { return unique; }
    }
    
    static class JdbcTableSchema implements TableSchema {
        private final String tableName;
        private final List<ColumnInfo> columns;
        private final List<IndexInfo> indexes;
        
        public JdbcTableSchema(String tableName, List<ColumnInfo> columns, List<IndexInfo> indexes) {
            this.tableName = tableName;
            this.columns = columns;
            this.indexes = indexes;
        }
        
        @Override
        public String getTableName() { return tableName; }
        @Override
        public List<ColumnInfo> getColumns() { return columns; }
        @Override
        public List<IndexInfo> getIndexes() { return indexes; }
    }
}