package com._1.core.exception;

import com._1.core.common.ResponseCodeEnum;
import com._1.core.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ResultVO<?> handleBusinessException(BusinessException ex) {
        log.error("BusinessException: code={}, message={}", ex.getCode(), ex.getMessage(), ex);
        return ResultVO.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResultVO<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation error (RequestBody): {}", message, ex);
        return ResultVO.validationFailed(message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Binding error: {}", message, ex);
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "参数绑定错误: " + message);
    }
    
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName(), ex);
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "缺少请求参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.warn("Parameter type mismatch: Parameter '{}' should be of type '{}'", ex.getName(), ex.getRequiredType().getSimpleName(), ex);
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "参数类型不匹配: '" + ex.getName() + "' 应为 " + ex.getRequiredType().getSimpleName() + " 类型");
    }
    
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResultVO<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMessage(), ex);
        return ResultVO.error(ResponseCodeEnum.ERROR.getCode(), "不支持的请求方法: " + ex.getMethod());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("HTTP message not readable: {}", ex.getMessage(), ex);
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "请求体格式错误或不可读");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResultVO<?> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage(), ex);
        return ResultVO.error(ResponseCodeEnum.FORBIDDEN);
    }
    
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultVO<?> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL(), ex);
        return ResultVO.error(ResponseCodeEnum.NOT_FOUND);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultVO<?> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException: {}", ex.getMessage(), ex);
        return ResultVO.error(ResponseCodeEnum.ERROR.getCode(), "服务器运行时发生未知错误，请联系管理员");
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResultVO<?> handleThrowable(Throwable ex) {
        log.error("Throwable: {}", ex.getMessage(), ex);
        return ResultVO.error(ResponseCodeEnum.ERROR.getCode(), "服务器发生严重错误，请联系管理员");
    }
} 