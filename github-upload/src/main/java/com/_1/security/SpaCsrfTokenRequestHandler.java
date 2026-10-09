package com._1.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * 同时支持两种 CSRF 令牌来源：
 * <ul>
 *   <li>前端（axios）和旧页面脚本：从 XSRF-TOKEN Cookie 读出原值，放进 X-XSRF-TOKEN 请求头；</li>
 *   <li>Thymeleaf 表单：由 th:action 自动插入的 _csrf 隐藏字段（经过 XOR 掩码）。</li>
 * </ul>
 * 写法来自 Spring Security 官方文档的单页应用示例。
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        // 让表单渲染时拿到带掩码的令牌，防 BREACH
        this.xor.handle(request, response, csrfToken);
        // 立即加载令牌，确保每个响应都能写出 XSRF-TOKEN Cookie
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
