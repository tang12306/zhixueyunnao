package com._1.controller;

import com._1.entity.User;
import com._1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.sql.DataSource;
import java.sql.Connection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = {"http://localhost:8082", "http://localhost:8083", "http://localhost:8084"})
public class TestController {

    @Autowired
    private UserService userService;

    @Autowired
    private DataSource dataSource;

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<User> users = userService.findAll();
            response.put("success", true);
            response.put("count", users.size());
            response.put("users", users.stream().limit(5).toList()); // 只返回前5个用户
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/cors")
    public ResponseEntity<Map<String, Object>> testCors() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "CORS配置正常");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-test")
    public ResponseEntity<Map<String, Object>> testLogin(@RequestParam String username,
                                                         @RequestParam String password,
                                                         @RequestParam String userType) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findByUsername(username).orElse(null);
            if (user != null) {
                response.put("success", true);
                response.put("message", "用户存在");
                response.put("username", user.getUsername());
                response.put("name", user.getName());
                response.put("role", user.getRole());
                response.put("enabled", user.getEnabled());
                response.put("passwordLength", user.getPassword() != null ? user.getPassword().length() : 0);
                response.put("passwordStartsWith", user.getPassword() != null ? user.getPassword().substring(0, Math.min(10, user.getPassword().length())) : "null");
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @PostMapping("/password-test")
    public ResponseEntity<Map<String, Object>> testPassword(@RequestParam String username,
                                                            @RequestParam String password) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findByUsername(username).orElse(null);
            if (user != null) {
                boolean matches = passwordEncoder.matches(password, user.getPassword());
                response.put("success", true);
                response.put("username", user.getUsername());
                response.put("passwordMatches", matches);
                response.put("inputPassword", password);
                response.put("storedPasswordLength", user.getPassword().length());
                response.put("storedPasswordPrefix", user.getPassword().substring(0, Math.min(20, user.getPassword().length())));
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/db-status")
    public ResponseEntity<Map<String, Object>> testDatabaseConnection() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 测试数据库连接
            List<User> users = userService.findAll();
            long userCount = users.size();

            // 统计不同角色的用户数量
            long teacherCount = users.stream().filter(u -> "TEACHER".equals(u.getRole())).count();
            long studentCount = users.stream().filter(u -> "STUDENT".equals(u.getRole())).count();

            response.put("success", true);
            response.put("message", "数据库连接正常");
            response.put("userCount", userCount);
            response.put("teacherCount", teacherCount);
            response.put("studentCount", studentCount);
            response.put("timestamp", new java.util.Date());
            response.put("databaseName", "jiaowu_tiku");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "数据库连接失败");
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/db-info")
    public ResponseEntity<Map<String, Object>> getDatabaseInfo() {
        Map<String, Object> response = new HashMap<>();
        try {
            Connection connection = dataSource.getConnection();
            String url = connection.getMetaData().getURL();
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
            String catalog = connection.getCatalog();

            response.put("success", true);
            response.put("url", url);
            response.put("databaseProductName", databaseProductName);
            response.put("databaseProductVersion", databaseProductVersion);
            response.put("currentCatalog", catalog);
            response.put("timestamp", new java.util.Date());

            connection.close();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }


}
