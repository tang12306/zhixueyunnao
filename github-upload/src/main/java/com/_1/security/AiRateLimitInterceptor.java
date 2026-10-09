package com._1.security;

import com._1.core.vo.ResultVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限制每个用户调用 AI 生成接口的频率（按自然分钟计数）。
 * 这些接口每次都会调用付费的大模型 API，不加限制容易被刷爆额度。
 * 计数只存在内存里，多实例部署时需要换成共享存储。
 */
@Component
public class AiRateLimitInterceptor implements HandlerInterceptor {

    /** 会调用大模型的接口，注册拦截器时使用 */
    public static final String[] AI_GENERATION_PATHS = {
            "/api/ai/generate-batch-questions",
            "/api/ai/generate-exam",
            "/api/generate-question"
    };

    private static final int PRUNE_THRESHOLD = 1_000;

    private final int limitPerMinute;
    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(long minute, int count) {
    }

    public AiRateLimitInterceptor(@Value("${app.ai.rate-limit-per-minute:5}") int limitPerMinute,
                                  ObjectMapper objectMapper) {
        this.limitPerMinute = limitPerMinute;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            // 未登录的请求已经被 Spring Security 拦下，这里不会走到
            return true;
        }

        long nowMillis = System.currentTimeMillis();
        long minute = nowMillis / 60_000;
        if (windows.size() > PRUNE_THRESHOLD) {
            windows.values().removeIf(window -> window.minute() != minute);
        }
        Window window = windows.compute(authentication.getName(), (user, previous) ->
                previous == null || previous.minute() != minute
                        ? new Window(minute, 1)
                        : new Window(minute, previous.count() + 1));

        if (window.count() <= limitPerMinute) {
            return true;
        }

        long retryAfterSeconds = Math.max(1, 60 - (nowMillis / 1000) % 60);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ResultVO.error(HttpStatus.TOO_MANY_REQUESTS.value(),
                "AI 生成请求太频繁，请 " + retryAfterSeconds + " 秒后再试"));
        return false;
    }
}
