package com._1.service.ai;

import com._1.service.ai.DeepSeekClient.ChatResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DeepSeekClientTests {

    private static final String URL = "https://deepseek.test/chat/completions";
    private static final String REPLY = """
            {"choices":[{"message":{"role":"assistant","content":"{\\"questions\\":[]}"},"finish_reason":"stop"}]}""";

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    private DeepSeekClient client(String key) {
        DeepSeekProperties properties = new DeepSeekProperties(URL, key, "deepseek-chat", 8000,
                Duration.ofSeconds(1), Duration.ofSeconds(1), 3, Duration.ZERO);
        return new DeepSeekClient(builder.build(), properties);
    }

    @Test
    void sendsJsonRequestAndReadsReply() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(jsonPath("$.model").value("deepseek-chat"))
                .andExpect(jsonPath("$.max_tokens").value(8000))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.response_format.type").value("json_object"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("含有 \"引号\" 和\n换行的提示词"))
                .andRespond(withSuccess(REPLY, MediaType.APPLICATION_JSON));

        ChatResult result = client("sk-test").chat("系统", "含有 \"引号\" 和\n换行的提示词", true);

        assertThat(result.content()).isEqualTo("{\"questions\":[]}");
        assertThat(result.truncated()).isFalse();
        server.verify();
    }

    @Test
    void omitsResponseFormatOutsideJsonModeAndReportsTruncation() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.response_format").doesNotExist())
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"半截"},"finish_reason":"length"}]}""", MediaType.APPLICATION_JSON));

        ChatResult result = client("sk-test").chat("系统", "用户", false);

        assertThat(result.content()).isEqualTo("半截");
        assertThat(result.truncated()).isTrue();
    }

    @Test
    void retriesServerErrorsThenSucceeds() {
        server.expect(requestTo(URL)).andRespond(withServerError());
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(URL)).andRespond(withSuccess(REPLY, MediaType.APPLICATION_JSON));

        assertThat(client("sk-test").chat("系统", "用户", true).content()).isEqualTo("{\"questions\":[]}");
        server.verify();
    }

    @Test
    void givesUpAfterMaxAttempts() {
        server.expect(ExpectedCount.times(3), requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AiServiceException e = catchThrowableOfType(() -> client("sk-test").chat("系统", "用户", true),
                AiServiceException.class);

        assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(e.getMessage()).contains("繁忙");
        server.verify();
    }

    @Test
    void doesNotRetryInvalidKey() {
        server.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        AiServiceException e = catchThrowableOfType(() -> client("sk-wrong").chat("系统", "用户", true),
                AiServiceException.class);

        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(e.getMessage()).contains("DEEPSEEK_API_KEY");
        server.verify();
    }

    @Test
    void doesNotRetryReadTimeout() {
        server.expect(ExpectedCount.once(), requestTo(URL)).andRespond(withException(new HttpTimeoutException("timed out")));

        AiServiceException e = catchThrowableOfType(() -> client("sk-test").chat("系统", "用户", true),
                AiServiceException.class);

        assertThat(e.getStatus()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        server.verify();
    }

    @Test
    void retriesConnectionErrors() {
        server.expect(ExpectedCount.times(3), requestTo(URL)).andRespond(withException(new ConnectException("refused")));

        AiServiceException e = catchThrowableOfType(() -> client("sk-test").chat("系统", "用户", true),
                AiServiceException.class);

        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(e.getMessage()).contains("无法连接");
        server.verify();
    }

    @Test
    void refusesToCallWithoutKey() {
        AiServiceException e = catchThrowableOfType(() -> client("  ").chat("系统", "用户", true),
                AiServiceException.class);

        assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(e.getMessage()).contains("DEEPSEEK_API_KEY");
        server.verify();
    }
}
