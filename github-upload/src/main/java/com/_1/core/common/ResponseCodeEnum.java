package com._1.core.common;

import lombok.Getter;

@Getter
public enum ResponseCodeEnum {
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败，服务器内部错误"),
    BAD_REQUEST(400, "错误的请求"),
    UNAUTHORIZED(401, "未经授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源未找到");

    private final int code;
    private final String message;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
} 