package com._1.core.exception;

import com._1.core.common.ResponseCodeEnum;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResponseCodeEnum responseCodeEnum) {
        super(responseCodeEnum.getMessage());
        this.code = responseCodeEnum.getCode();
    }

    public BusinessException(ResponseCodeEnum responseCodeEnum, String customMessage) {
        super(customMessage);
        this.code = responseCodeEnum.getCode();
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = ResponseCodeEnum.ERROR.getCode();
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = ResponseCodeEnum.ERROR.getCode();
    }
    
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
} 