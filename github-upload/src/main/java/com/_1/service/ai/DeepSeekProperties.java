package com._1.service.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * DeepSeek 接口配置，对应 application.properties 里的 deepseek.api.*。
 *
 * @param url            Chat Completions 地址
 * @param key            API 密钥，来自环境变量 DEEPSEEK_API_KEY
 * @param model          模型名
 * @param maxTokens      单次回复的最大 token 数；一套试卷 30 道题大约需要 6000
 * @param connectTimeout 连接超时
 * @param readTimeout    等待回复的超时，生成整套试卷可能要 1～2 分钟
 * @param maxAttempts    网络错误、429 和 5xx 时最多尝试几次
 * @param retryBackoff   第 n 次重试前等待 n × retryBackoff
 */
@ConfigurationProperties(prefix = "deepseek.api")
public record DeepSeekProperties(
        @DefaultValue("https://api.deepseek.com/chat/completions") String url,
        @DefaultValue("") String key,
        @DefaultValue("deepseek-chat") String model,
        @DefaultValue("8000") int maxTokens,
        @DefaultValue("10s") Duration connectTimeout,
        @DefaultValue("180s") Duration readTimeout,
        @DefaultValue("3") int maxAttempts,
        @DefaultValue("2s") Duration retryBackoff) {

    public boolean isConfigured() {
        return key != null && !key.isBlank();
    }
}
