package com._1.service.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;

/**
 * 项目里唯一调用 DeepSeek 的地方。请求体由 Jackson 序列化，日志只记录长度和耗时，不记录提示词和回复内容。
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final RestClient restClient;
    private final DeepSeekProperties properties;

    @Autowired
    public DeepSeekClient(RestClient.Builder builder, DeepSeekProperties properties) {
        this(builder.requestFactory(requestFactory(properties)).build(), properties);
    }

    /** 测试用：传入绑定了 MockRestServiceServer 的 RestClient */
    DeepSeekClient(RestClient restClient, DeepSeekProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    private static JdkClientHttpRequestFactory requestFactory(DeepSeekProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    /**
     * 发送一轮对话。
     *
     * @param jsonMode 为 true 时要求模型只输出 JSON 对象（提示词里必须出现 "json" 字样并给出示例）
     */
    public ChatResult chat(String systemPrompt, String userPrompt, boolean jsonMode) {
        if (!properties.isConfigured()) {
            throw AiServiceException.notConfigured();
        }
        ChatRequest body = new ChatRequest(
                properties.model(),
                List.of(new Message("system", systemPrompt), new Message("user", userPrompt)),
                0.7,
                properties.maxTokens(),
                false,
                jsonMode ? new ResponseFormat("json_object") : null);

        int maxAttempts = Math.max(1, properties.maxAttempts());
        long started = System.currentTimeMillis();
        for (int attempt = 1; ; attempt++) {
            try {
                JsonNode response = restClient.post()
                        .uri(properties.url())
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(headers -> headers.setBearerAuth(properties.key()))
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                JsonNode choice = response == null ? null : response.path("choices").path(0);
                String content = choice == null ? "" : choice.path("message").path("content").asText("");
                boolean truncated = choice != null && "length".equals(choice.path("finish_reason").asText());
                log.info("DeepSeek 调用完成：提示词 {} 字符，回复 {} 字符，截断={}，耗时 {} ms，第 {} 次尝试",
                        userPrompt.length(), content.length(), truncated,
                        System.currentTimeMillis() - started, attempt);
                return new ChatResult(content, truncated);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                log.warn("DeepSeek 返回 HTTP {}（第 {}/{} 次尝试）：{}", status, attempt, maxAttempts,
                        abbreviate(e.getResponseBodyAsString(), 300));
                boolean retryable = status == 429 || status >= 500;
                if (!retryable || attempt >= maxAttempts) {
                    throw AiServiceException.upstream(status, e);
                }
            } catch (ResourceAccessException e) {
                boolean readTimeout = isReadTimeout(e);
                log.warn("DeepSeek 网络错误（第 {}/{} 次尝试）：{}", attempt, maxAttempts,
                        readTimeout ? "等待回复超时" : e.getMessage());
                // 读超时说明已经等了很久，再重试只会让教师等更久
                if (readTimeout) {
                    throw AiServiceException.timeout(e);
                }
                if (attempt >= maxAttempts) {
                    throw AiServiceException.unreachable(e);
                }
            }
            sleep(properties.retryBackoff().toMillis() * attempt);
        }
    }

    private static boolean isReadTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof HttpConnectTimeoutException) {
                return false;
            }
            if (t instanceof HttpTimeoutException || t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 请求已取消", null, e);
        }
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    /** 模型回复。truncated 表示回复因 max_tokens 被截断，JSON 多半不完整 */
    public record ChatResult(String content, boolean truncated) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ChatRequest(
            String model,
            List<Message> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens,
            boolean stream,
            @JsonProperty("response_format") ResponseFormat responseFormat) {
    }

    record Message(String role, String content) {
    }

    record ResponseFormat(String type) {
    }
}
