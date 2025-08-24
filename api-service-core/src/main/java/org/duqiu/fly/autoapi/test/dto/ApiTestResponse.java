package org.duqiu.fly.autoapi.test.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ApiTestResponse {
    private boolean success;
    private Object data;
    private String message;
    private long executionTime;
    private int statusCode;
    private String errorDetail;
    private LocalDateTime testTime;
    private Map<String, Object> testParameters;
    private int recordCount;
    private long executionTimeMs;
    private String errorMessage;
    private String errorCode;
    private SqlExecutionDetail sqlExecutionDetail;
    
    @Data
    public static class SqlExecutionDetail {
        private String executedSql;
        private Long connectionTimeMs;
        private Long queryTimeMs;
        private String dataSourceType;
        private String dataSourceName;
        private Map<String, Object> sqlParameters;
        private Long processingTimeMs;
    }
    
    public static ApiTestResponse success(Object data, long executionTime) {
        ApiTestResponse response = new ApiTestResponse();
        response.setSuccess(true);
        response.setData(data);
        response.setExecutionTime(executionTime);
        response.setStatusCode(200);
        response.setMessage("测试成功");
        return response;
    }
    
    public static ApiTestResponse error(String message, String errorDetail, long executionTime) {
        ApiTestResponse response = new ApiTestResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorDetail(errorDetail);
        response.setExecutionTime(executionTime);
        response.setStatusCode(500);
        return response;
    }
}