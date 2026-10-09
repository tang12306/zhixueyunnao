package com._1.core.exception;

import com._1.core.common.ResponseCodeEnum;
import com._1.core.vo.ResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 接口错误统一在这里转换：HTTP 状态码就是真实结果，响应体是 {code, success:false, message, data}，
 * code 与状态码相同。message 只放给用户看的固定文案，不把异常原文返回给前端。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ResultVO<?>> handleApiException(ApiException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.warn("ApiException {}: {} {}", ex.getStatus().value(), ex.getMessage(),
                    ex.getCause() != null ? ex.getCause().toString() : "");
        } else {
            log.debug("ApiException {}: {}", ex.getStatus().value(), ex.getMessage());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(ResultVO.error(ex.getStatus().value(), ex.getMessage(), ex.getData()));
    }

    /** 参数不合法（找不到学科、未知题型等），消息本身就是给用户看的 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), ex.getMessage());
    }

    /** 唯一约束、外键约束冲突：例如重名，或者删除仍被引用的数据 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResultVO<?> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResultVO.error(HttpStatus.CONFLICT.value(), "数据重复或仍被其他数据引用，操作未完成");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        // 校验注解里的 message 已经是给用户看的中文，只拼接消息，不暴露字段名
        String message = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        log.debug("Validation error (RequestBody): {}", message);
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleBindException(BindException ex) {
        String fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField())
                .distinct()
                .collect(Collectors.joining(", "));
        log.debug("Binding error: {}", fields);
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "参数格式不正确: " + fields);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.debug("Missing request parameter: {}", ex.getParameterName());
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "缺少请求参数: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.debug("Parameter type mismatch: {}", ex.getName());
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "参数格式不正确: " + ex.getName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ResultVO<?> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.debug("HTTP method not supported: {}", ex.getMethod());
        return ResultVO.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "不支持的请求方法: " + ex.getMethod());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResultVO<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.debug("HTTP message not readable: {}", ex.getMessage());
        return ResultVO.error(ResponseCodeEnum.BAD_REQUEST.getCode(), "请求体格式错误或不可读");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResultVO<?> handleAccessDeniedException(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        return ResultVO.error(ResponseCodeEnum.FORBIDDEN);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultVO<?> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.debug("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return ResultVO.error(ResponseCodeEnum.NOT_FOUND);
    }

    // Spring Boot 3.2 起，不存在的路径抛的是这个异常；不单独处理会落到下面的 Throwable，变成 500
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResultVO<?> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.debug("No resource found: {}", ex.getResourcePath());
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
