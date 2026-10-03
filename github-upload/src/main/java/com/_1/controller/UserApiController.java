package com._1.controller;

import com._1.entity.User;
import com._1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的信息
     * @return 用户信息
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOptional = userService.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole());
            response.put("user", userMap);
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 更新当前用户的个人信息
     * @param userData 用户数据
     * @return 更新结果
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> userData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOptional = userService.findByUsername(username);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            
            // 更新用户信息
            if (userData.containsKey("name")) {
                user.setName(userData.get("name"));
            }
            if (userData.containsKey("email")) {
                user.setEmail(userData.get("email"));
            }
            
            // 保存更新后的用户信息
            userService.save(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("name", user.getName());
            userMap.put("email", user.getEmail());
            userMap.put("role", user.getRole());
            response.put("user", userMap);
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 修改当前用户的密码
     * @param passwordData 密码数据
     * @return 修改结果
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "旧密码和新密码不能为空");
            return ResponseEntity.ok(response);
        }
        
        boolean result = userService.changePassword(username, oldPassword, newPassword);
        
        Map<String, Object> response = new HashMap<>();
        if (result) {
            response.put("success", true);
            response.put("message", "密码修改成功");
        } else {
            response.put("success", false);
            response.put("message", "密码修改失败，请确认旧密码是否正确");
        }
        return ResponseEntity.ok(response);
    }
} 