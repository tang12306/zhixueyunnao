package com._1.controller;

import com._1.core.common.Roles;
import com._1.core.exception.ApiException;
import com._1.entity.User;
import com._1.security.LoginAttemptService;
import com._1.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 前端（Vue）的登录接口。登录成功后通过会话 Cookie（JSESSIONID，HttpOnly）保持登录状态。
 * 学生端暂未开放，学生账号在这里会被拒绝。
 */
@RestController
@RequestMapping("/api/auth")
public class ApiLoginController {

    private static final Logger logger = LoggerFactory.getLogger(ApiLoginController.class);
    private static final Set<String> STAFF_AUTHORITIES = Set.of("ROLE_" + Roles.TEACHER, "ROLE_" + Roles.ADMIN);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public ApiLoginController(AuthenticationManager authenticationManager, UserService userService,
                              LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.loginAttemptService = loginAttemptService;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest body,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        String username = body.username() == null ? "" : body.username().trim();
        String password = body.password() == null ? "" : body.password();
        if (username.isEmpty() || password.isEmpty()) {
            throw ApiException.badRequest("请输入用户名和密码");
        }

        String attemptKey = LoginAttemptService.key(username, request.getRemoteAddr());
        if (loginAttemptService.isLocked(attemptKey)) {
            logger.warn("登录被锁定: username={}, ip={}", username, request.getRemoteAddr());
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "登录失败次数过多，请 " + loginAttemptService.lockMinutes() + " 分钟后再试");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        } catch (AuthenticationException e) {
            // 用户不存在、密码错误、账号停用统一返回同一句话，避免被用来探测账号
            loginAttemptService.loginFailed(attemptKey);
            logger.warn("登录失败: username={}, ip={}, reason={}", username, request.getRemoteAddr(),
                    e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        loginAttemptService.loginSucceeded(attemptKey);

        boolean staff = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(STAFF_AUTHORITIES::contains);
        if (!staff) {
            throw new ApiException(HttpStatus.FORBIDDEN, "学生端暂未开放，请使用教师或管理员账号登录");
        }

        // 防会话固定：登录前已有会话时换一个新的会话 ID
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        logger.info("用户登录成功: username={}", username);
        return userService.findByUsername(authentication.getName())
                .map(user -> ok("登录成功", user))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
    }

    /** 当前登录用户；未登录时由 Spring Security 返回 401 */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        return userService.findByUsername(authentication.getName())
                .map(user -> ok(null, user))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "请先登录"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request, HttpServletResponse response,
                                                      Authentication authentication) {
        logoutHandler.logout(request, response, authentication);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "已退出登录");
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<Map<String, Object>> ok(String message, User user) {
        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("role", user.getRole());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        if (message != null) {
            body.put("message", message);
        }
        body.put("user", userMap);
        return ResponseEntity.ok(body);
    }
}
