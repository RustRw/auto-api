package org.duqiu.fly.autoapi.datasource.nosql;

import com.mongodb.client.*;
import org.bson.Document;
import org.duqiu.fly.autoapi.datasource.core.DataSourceConnection;

import java.util.*;

/**
 * MongoDB连接实现
 */
public class MongoConnection implements DataSourceConnection {
    
    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final String databaseName;
    
    public MongoConnection(MongoClient mongoClient, String databaseName) {
        this.mongoClient = mongoClient;
        this.databaseName = databaseName;
        this.database = mongoClient.getDatabase(databaseName);
    }
    
    @Override
    public boolean isValid() {
        try {
            // 执行ping命令测试连接
            database.runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public QueryResult executeQuery(String query, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        try {
            // 解析MongoDB查询
            MongoQuery mongoQuery = parseMongoQuery(query, parameters);
            
            MongoCollection<Document> collection = database.getCollection(mongoQuery.getCollectionName());
            List<Map<String, Object>> data = new ArrayList<>();
            
            FindIterable<Document> iterable;
            if (mongoQuery.getFilter().isEmpty()) {
                iterable = collection.find();
            } else {
                iterable = collection.find(mongoQuery.getFilter());
            }
            
            // 应用投影
            if (!mongoQuery.getProjection().isEmpty()) {
                iterable = iterable.projection(mongoQuery.getProjection());
            }
            
            // 应用排序
            if (!mongoQuery.getSort().isEmpty()) {
                iterable = iterable.sort(mongoQuery.getSort());
            }
            
            // 应用分页
            if (mongoQuery.getSkip() > 0) {
                iterable = iterable.skip(mongoQuery.getSkip());
            }
            
            if (mongoQuery.getLimit() > 0) {
                iterable = iterable.limit(mongoQuery.getLimit());
            }
            
            // 收集结果
            for (Document doc : iterable) {
                data.add(documentToMap(doc));
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 获取列信息（MongoDB的字段是动态的）
            List<ColumnInfo> columns = extractColumnsFromData(data);
            
            return new MongoQueryResult(data, data.size(), columns, executionTime, true, null);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new MongoQueryResult(Collections.emptyList(), 0, Collections.emptyList(),
                                       executionTime, false, e.getMessage());
        }
    }
    
    @Override
    public UpdateResult executeUpdate(String command, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        try {
            // 解析MongoDB更新命令
            MongoUpdateCommand updateCommand = parseMongoUpdateCommand(command, parameters);
            
            MongoCollection<Document> collection = database.getCollection(updateCommand.getCollectionName());
            long affectedRows = 0;
            
            switch (updateCommand.getOperation()) {
                case "insertOne":
                    collection.insertOne(updateCommand.getDocument());
                    affectedRows = 1;
                    break;
                case "insertMany":
                    collection.insertMany(updateCommand.getDocuments());
                    affectedRows = updateCommand.getDocuments().size();
                    break;
                case "updateOne":
                    affectedRows = collection.updateOne(updateCommand.getFilter(), 
                                                       updateCommand.getUpdate()).getModifiedCount();
                    break;
                case "updateMany":
                    affectedRows = collection.updateMany(updateCommand.getFilter(), 
                                                        updateCommand.getUpdate()).getModifiedCount();
                    break;
                case "deleteOne":
                    affectedRows = collection.deleteOne(updateCommand.getFilter()).getDeletedCount();
                    break;
                case "deleteMany":
                    affectedRows = collection.deleteMany(updateCommand.getFilter()).getDeletedCount();
                    break;
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            return new MongoUpdateResult(affectedRows, executionTime, true, null);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return new MongoUpdateResult(0, executionTime, false, e.getMessage());
        }
    }
    
    @Override
    public ConnectionInfo getConnectionInfo() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("databaseName", databaseName);
        properties.put("clientType", "MongoDB Java Driver");
        
        // 获取服务器信息
        try {
            Document serverStatus = database.runCommand(new Document("serverStatus", 1));
            properties.put("version", serverStatus.getString("version"));
            properties.put("host", serverStatus.getString("host"));
        } catch (Exception e) {
            properties.put("version", "unknown");
        }
        
        return new MongoConnectionInfo("mongodb://", 
                                      (String) properties.get("version"), properties);
    }
    
    @Override
    public List<TableInfo> getTables() {
        List<TableInfo> tables = new ArrayList<>();
        try {
            for (String collectionName : database.listCollectionNames()) {
                tables.add(new MongoTableInfo(collectionName, "COLLECTION", null));
            }
        } catch (Exception e) {
            // 日志记录错误
        }
        return tables;
    }
    
    @Override
    public TableSchema getTableSchema(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        
        try {
            MongoCollection<Document> collection = database.getCollection(tableName);
            
            // 样本文档来推断schema
            FindIterable<Document> sample = collection.find().limit(100);
            Set<String> fieldNames = new HashSet<>();
            
            for (Document doc : sample) {
                extractFieldNames(doc, "", fieldNames);
            }
            
            for (String fieldName : fieldNames) {
                columns.add(new MongoColumnInfo(fieldName, "Object", true, null, null));
            }
            
        } catch (Exception e) {
            // 日志记录错误
        }
        
        return new MongoTableSchema(tableName, columns, Collections.emptyList());
    }
    
    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
    
    private Map<String, Object> documentToMap(Document doc) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
    
    private List<ColumnInfo> extractColumnsFromData(List<Map<String, Object>> data) {
        Set<String> fieldNames = new HashSet<>();
        for (Map<String, Object> row : data) {
            fieldNames.addAll(row.keySet());
        }
        
        List<ColumnInfo> columns = new ArrayList<>();
        for (String fieldName : fieldNames) {
            columns.add(new MongoColumnInfo(fieldName, "Object", true, null, null));
        }
        return columns;
    }
    
    private void extractFieldNames(Document doc, String prefix, Set<String> fieldNames) {
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            String fieldName = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            fieldNames.add(fieldName);
            
            if (entry.getValue() instanceof Document) {
                extractFieldNames((Document) entry.getValue(), fieldName, fieldNames);
            }
        }
    }
    
    private MongoQuery parseMongoQuery(String query, Map<String, Object> parameters) {
        // 简化的MongoDB查询解析
        // 实际应该实现完整的查询解析器
        MongoQuery mongoQuery = new MongoQuery();
        mongoQuery.setCollectionName("test"); // 默认集合名
        mongoQuery.setFilter(new Document());
        mongoQuery.setProjection(new Document());
        mongoQuery.setSort(new Document());
        mongoQuery.setSkip(0);
        mongoQuery.setLimit(0);
        return mongoQuery;
    }
    
    private MongoUpdateCommand parseMongoUpdateCommand(String command, Map<String, Object> parameters) {
        // 简化的MongoDB更新命令解析
        MongoUpdateCommand updateCommand = new MongoUpdateCommand();
        updateCommand.setCollectionName("test");
        updateCommand.setOperation("insertOne");
        updateCommand.setDocument(new Document(parameters));
        return updateCommand;
    }
    
    // 内部类定义...
    static class MongoQuery {
        private String collectionName;
        private Document filter;
        private Document projection;
        private Document sort;
        private int skip;
        private int limit;
        
        // getters and setters
        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
        public Document getFilter() { return filter; }
        public void setFilter(Document filter) { this.filter = filter; }
        public Document getProjection() { return projection; }
        public void setProjection(Document projection) { this.projection = projection; }
        public Document getSort() { return sort; }
        public void setSort(Document sort) { this.sort = sort; }
        public int getSkip() { return skip; }
        public void setSkip(int skip) { this.skip = skip; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
    
    static class MongoUpdateCommand {
        private String collectionName;
        private String operation;
        private Document filter;
        private Document update;
        private Document document;
        private List<Document> documents;
        
        // getters and setters
        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public Document getFilter() { return filter; }
        public void setFilter(Document filter) { this.filter = filter; }
        public Document getUpdate() { return update; }
        public void setUpdate(Document update) { this.update = update; }
        public Document getDocument() { return document; }
        public void setDocument(Document document) { this.document = document; }
        public List<Document> getDocuments() { return documents; }
        public void setDocuments(List<Document> documents) { this.documents = documents; }
    }
    
    // Result实现类...
    static class MongoQueryResult implements QueryResult {
        private final List<Map<String, Object>> data;
        private final long count;
        private final List<ColumnInfo> columns;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public MongoQueryResult(List<Map<String, Object>> data, long count, List<ColumnInfo> columns,
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
    
    static class MongoUpdateResult implements UpdateResult {
        private final long affectedRows;
        private final long executionTime;
        private final boolean success;
        private final String errorMessage;
        
        public MongoUpdateResult(long affectedRows, long executionTime, boolean success, String errorMessage) {
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
    
    // 其他内部类...
    static class MongoConnectionInfo implements ConnectionInfo {
        private final String url;
        private final String version;
        private final Map<String, Object> properties;
        
        public MongoConnectionInfo(String url, String version, Map<String, Object> properties) {
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
    
    static class MongoTableInfo implements TableInfo {
        private final String name;
        private final String type;
        private final String comment;
        
        public MongoTableInfo(String name, String type, String comment) {
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
    
    static class MongoColumnInfo implements ColumnInfo {
        private final String name;
        private final String type;
        private final boolean nullable;
        private final String comment;
        private final Object defaultValue;
        
        public MongoColumnInfo(String name, String type, boolean nullable, String comment, Object defaultValue) {
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
    
    static class MongoTableSchema implements TableSchema {
        private final String tableName;
        private final List<ColumnInfo> columns;
        private final List<IndexInfo> indexes;
        
        public MongoTableSchema(String tableName, List<ColumnInfo> columns, List<IndexInfo> indexes) {
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