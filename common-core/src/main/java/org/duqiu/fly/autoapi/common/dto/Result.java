package org.duqiu.fly.autoapi.common.dto;

import lombok.Data;

@Data
public class Result<T> {
    
    private boolean success;
    private String message;
    private T data;
    private String code;

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.success = true;
        result.message = "成功";
        result.data = data;
        result.code = "200";
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.success = false;
        result.message = message;
        result.code = "500";
        return result;
    }

    public static <T> Result<T> error(String message, String code) {
        Result<T> result = new Result<>();
        result.success = false;
        result.message = message;
        result.code = code;
        return result;
    }
}