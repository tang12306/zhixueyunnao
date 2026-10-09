package com._1.core.vo;

import com._1.core.common.ResponseCodeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "统一API响应结果封装")
public class ResultVO<T> {

    @Schema(description = "状态码，例如 200 表示成功", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer code;

    @Schema(description = "响应消息，例如 '操作成功'", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "响应数据")
    private T data;

    // 构造函数
    private ResultVO(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    private ResultVO(Integer code, String message) {
        this(code, message, null);
    }

    // 静态成功方法
    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(ResponseCodeEnum.SUCCESS.getCode(), ResponseCodeEnum.SUCCESS.getMessage(), data);
    }

    public static <T> ResultVO<T> success() {
        return success(null);
    }

    public static <T> ResultVO<T> success(String message, T data) {
        return new ResultVO<>(ResponseCodeEnum.SUCCESS.getCode(), message, data);
    }

    public static <T> ResultVO<T> success(Integer code, String message, T data) {
        return new ResultVO<>(code, message, data);
    }

    // 静态失败方法
    public static <T> ResultVO<T> error(Integer code, String message) {
        return new ResultVO<>(code, message);
    }

    public static <T> ResultVO<T> error(Integer code, String message, T data) {
        return new ResultVO<>(code, message, data);
    }

    public static <T> ResultVO<T> error(ResponseCodeEnum errorCode) {
        return new ResultVO<>(errorCode.getCode(), errorCode.getMessage());
    }
    
    public static <T> ResultVO<T> error(ResponseCodeEnum errorCode, String customMessage) {
        return new ResultVO<>(errorCode.getCode(), customMessage);
    }

    public static <T> ResultVO<T> error(String message) {
        return new ResultVO<>(ResponseCodeEnum.ERROR.getCode(), message);
    }

    /**
     * 很多页面按 response.success 判断结果，这里和 code 保持一致
     */
    @Schema(description = "是否成功，等价于 code 为 2xx")
    public boolean isSuccess() {
        return code != null && code >= 200 && code < 300;
    }
}
