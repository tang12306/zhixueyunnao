package com._1.controller;

import com._1.entity.User;
import com._1.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:8082", "http://localhost:8083", "http://localhost:8084"})
public class ApiLoginController {

    private static final Logger logger = LoggerFactory.getLogger(ApiLoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestParam String username,
                                                     @RequestParam String password,
                                                     @RequestParam String userType,
                                                     HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("API登录尝试: username={}, userType={}", username, userType);
            
            // 查找用户
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                logger.warn("用户不存在: {}", username);
                response.put("success", false);
                response.put("message", "用户名或密码错误");
                return ResponseEntity.ok(response);
            }
            
            User user = userOpt.get();
            
            // 验证密码
            if (!passwordEncoder.matches(password, user.getPassword())) {
                logger.warn("密码错误: {}", username);
                response.put("success", false);
                response.put("message", "用户名或密码错误");
                return ResponseEntity.ok(response);
            }
            
            // 验证用户类型与角色匹配 - 只允许教师登录
            if (!"teacher".equals(userType)) {
                logger.warn("不支持的用户类型: username={}, userType={}", username, userType);
                response.put("success", false);
                response.put("message", "当前只支持教师登录");
                return ResponseEntity.ok(response);
            }

            if (!"TEACHER".equals(user.getRole())) {
                logger.warn("用户角色不匹配: username={}, actualRole={}", username, user.getRole());
                response.put("success", false);
                response.put("message", "用户类型选择错误");
                return ResponseEntity.ok(response);
            }
            
            // 执行Spring Security认证
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(authToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 创建会话
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            
            logger.info("用户登录成功: username={}, role={}", username, user.getRole());
            
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("user", Map.of(
                "username", user.getUsername(),
                "name", user.getName(),
                "role", user.getRole()
            ));
            // 只支持教师登录，重定向到教师主页
            response.put("redirectUrl", "/");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("登录过程中发生错误: username={}", username, e);
            response.put("success", false);
            response.put("message", "登录失败，请稍后重试");
            return ResponseEntity.ok(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            
            response.put("success", true);
            response.put("message", "退出登录成功");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("退出登录过程中发生错误", e);
            response.put("success", false);
            response.put("message", "退出登录失败");
            return ResponseEntity.ok(response);
        }
    }
}
