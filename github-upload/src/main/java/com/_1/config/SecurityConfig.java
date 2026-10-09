package com._1.config;

import com._1.core.common.Roles;
import com._1.core.vo.ResultVO;
import com._1.security.SpaCsrfTokenRequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 访问控制：默认拒绝。
 * <ul>
 *   <li>匿名只能访问静态资源、登录页和登录接口；</li>
 *   <li>管理接口和旧版页面要求 TEACHER 或 ADMIN；系统设置的修改、旧版用户管理只给 ADMIN；</li>
 *   <li>/student/** 为预留的学生端，只给 STUDENT。</li>
 * </ul>
 * 前端（Vue）用会话 Cookie 登录，经同源代理访问后端；写操作需要带 X-XSRF-TOKEN 请求头。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 这些请求出错时返回 JSON（401/403），而不是重定向到登录页 */
    private static final RequestMatcher API_REQUEST = new OrRequestMatcher(
            new AntPathRequestMatcher("/api/**"),
            new AntPathRequestMatcher("/questions/api/**"),
            new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final ObjectMapper objectMapper;

    public SecurityConfig(CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                          ObjectMapper objectMapper) {
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(authorize -> authorize
                // 公开资源
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/login", "/error", "/error/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()

                // 登录后可用：当前用户信息、个人资料、修改自己的密码
                .requestMatchers("/api/auth/**", "/api/user/**").authenticated()
                .requestMatchers("/settings/changePassword").authenticated()

                // 仅管理员：修改系统设置、旧版页面的用户管理
                .requestMatchers(HttpMethod.GET, "/api/settings/**").hasAnyRole(Roles.TEACHER, Roles.ADMIN)
                .requestMatchers("/api/settings/**").hasRole(Roles.ADMIN)
                .requestMatchers("/settings/**").hasRole(Roles.ADMIN)

                // 学生端（预留，暂未开放）
                .requestMatchers("/student/**").hasRole(Roles.STUDENT)

                // 其余一律要求教师或管理员（包括 /api/**、/questions/**、API 文档和旧版页面）
                .anyRequest().hasAnyRole(Roles.TEACHER, Roles.ADMIN)
            )
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(jsonAuthenticationEntryPoint(), API_REQUEST)
                .defaultAccessDeniedHandlerFor(jsonAccessDeniedHandler(), API_REQUEST)
            )
            // 旧版 Thymeleaf 页面的表单登录
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customAuthenticationSuccessHandler)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                // 登录接口只收 application/json，跨站表单发不出这种请求，跨站脚本又会被 CORS 预检拦下
                .ignoringRequestMatchers(new AntPathRequestMatcher("/api/auth/login", HttpMethod.POST.name()))
            );

        return http.build();
    }

    /**
     * 允许跨域的前端地址，来自配置 app.cors.allowed-origins。
     * 前端通过同源代理（开发时 vue devServer、上线时 Nginx）访问时用不到。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:8083}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
        config.setAllowCredentials(true);
        config.setAllowedHeaders(List.of("Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setExposedHeaders(List.of("Content-Disposition", "Retry-After"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, authException) ->
                writeJson(response, HttpStatus.UNAUTHORIZED, "请先登录");
    }

    private AccessDeniedHandler jsonAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            String message = accessDeniedException instanceof CsrfException
                    ? "页面已过期，请刷新后重试"
                    : "没有权限执行此操作";
            writeJson(response, HttpStatus.FORBIDDEN, message);
        };
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ResultVO.error(status.value(), message));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

}
