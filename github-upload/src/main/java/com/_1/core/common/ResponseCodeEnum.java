package com._1.core.common;

import lombok.Getter;

@Getter
public enum ResponseCodeEnum {
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败，服务器内部错误"),
    BAD_REQUEST(400, "错误的请求"),
    UNAUTHORIZED(401, "未经授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源未找到"),
    VALIDATION_ERROR(422, "参数校验失败"), // Unprocessable Entity

    // 业务相关错误码可以从1000开始，例如
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_PASSWORD_ERROR(1002, "密码错误"),
    USERNAME_ALREADY_EXISTS(1003, "用户名已存在"),

    QUESTION_NOT_FOUND(2001, "题目不存在");
    // ...更多业务相关的状态码

    private final int code;
    private final String message;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
} 