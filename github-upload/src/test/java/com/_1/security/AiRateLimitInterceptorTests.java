package com._1.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class AiRateLimitInterceptorTests {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksRequestsOverTheLimitPerUser() throws Exception {
        AiRateLimitInterceptor interceptor = new AiRateLimitInterceptor(2, new ObjectMapper());

        loginAs("teacher.a");
        assertThat(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), null)).isTrue();
        assertThat(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), null)).isTrue();

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(new MockHttpServletRequest(), blocked, null)).isFalse();
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotBlank();
        assertThat(blocked.getContentAsString()).contains("\"success\":false");

        // 其他用户不受影响
        loginAs("teacher.b");
        assertThat(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), null)).isTrue();
    }

    private static void loginAs(String username) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(username, "n/a", "ROLE_TEACHER");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
