package com.gk.jobhelper.common;

/**
 * 统一 REST 响应结构
 * code: 0-成功; 40000-参数/业务错误; 40401-档案不存在; 40402-档案已存在;
 *       40403-导入记录不存在; 40404-岗位不存在; 50000-系统内部错误
 */
public class ApiResponse<T> {

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_BAD_REQUEST = 40000;
    public static final int CODE_PROFILE_NOT_FOUND = 40401;
    public static final int CODE_PROFILE_EXISTS = 40402;
    public static final int CODE_IMPORT_NOT_FOUND = 40403;
    public static final int CODE_JOB_NOT_FOUND = 40404;
    public static final int CODE_MATCH_NOT_FOUND = 40405;
    public static final int CODE_INTERNAL_ERROR = 50000;

    private int code;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(CODE_SUCCESS, "success", data);
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
