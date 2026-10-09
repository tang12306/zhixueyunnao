package com._1.service.ai;

import com._1.core.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * AI 调用或解析失败。message 可以直接展示给教师；解析失败时响应的 data.raw 带上模型的原始输出，
 * 方便教师看到 AI 实际返回了什么。
 */
public class AiServiceException extends ApiException {

    private final String raw;

    public AiServiceException(HttpStatus status, String message, String raw, Throwable cause) {
        super(status, message, raw == null ? null : Map.of("raw", raw), cause);
        this.raw = raw;
    }

    public static AiServiceException notConfigured() {
        return new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI 服务未配置：请在 .env 中设置 DEEPSEEK_API_KEY 后重启后端", null, null);
    }

    public static AiServiceException upstream(int upstreamStatus, Throwable cause) {
        String message = switch (upstreamStatus) {
            case 401, 403 -> "AI 服务密钥无效，请检查 DEEPSEEK_API_KEY";
            case 402 -> "AI 服务账户余额不足";
            case 429 -> "AI 服务繁忙，请稍后重试";
            case 400, 422 -> "AI 服务拒绝了这次请求，请减少题目数量或修改要求后重试";
            default -> "AI 服务暂时不可用，请稍后重试";
        };
        HttpStatus status = upstreamStatus == 429 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        return new AiServiceException(status, message, null, cause);
    }

    public static AiServiceException unreachable(Throwable cause) {
        return new AiServiceException(HttpStatus.BAD_GATEWAY, "无法连接 AI 服务，请检查服务器网络", null, cause);
    }

    public static AiServiceException timeout(Throwable cause) {
        return new AiServiceException(HttpStatus.GATEWAY_TIMEOUT, "AI 服务响应超时，请减少题目数量后重试", null, cause);
    }

    public static AiServiceException unparseable(String message, String raw) {
        return new AiServiceException(HttpStatus.BAD_GATEWAY, message, raw, null);
    }

    public String getRaw() {
        return raw;
    }
}
