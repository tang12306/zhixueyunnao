package com._1.core.exception;

import org.springframework.http.HttpStatus;

/**
 * 带 HTTP 状态码的业务异常。GlobalExceptionHandler 按 status 返回，
 * 响应体是 {code, success:false, message, data}，message 可以直接展示给用户。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final transient Object data;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null, null);
    }

    public ApiException(HttpStatus status, String message, Object data, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.data = data;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }
}
