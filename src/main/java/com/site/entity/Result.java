package com.site.entity;

import lombok.Data;

@Data
public class Result {
    private int code;
    private String message;
    private Object data;

    public static Result success() {
        Result result = new Result();
        result.setCode(0);
        return result;
    }

    public static Result success(Object data) {
        Result result = new Result();
        result.setCode(0);
        result.setData(data);
        return result;
    }

    public static Result error(String message) {
        Result result = new Result();
        result.setCode(1);
        result.setMessage(message);
        return result;
    }
} 