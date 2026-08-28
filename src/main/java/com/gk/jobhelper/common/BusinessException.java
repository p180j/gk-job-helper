package com.gk.jobhelper.common;

/**
 * 业务异常，携带返回给前端的错误码与提示信息
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        this(ApiResponse.CODE_BAD_REQUEST, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
