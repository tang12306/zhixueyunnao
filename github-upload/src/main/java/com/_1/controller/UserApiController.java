package com._1.controller;

import com._1.core.exception.ApiException;
import com._1.entity.User;
import com._1.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    private final UserService userService;

    public UserApiController(UserService userService) {
        this.userService = userService;
    }

    /** 当前登录用户的信息 */
    @GetMapping("/current")
    public Map<String, Object> getCurrentUser(Authentication authentication) {
        return userResponse(null, currentUser(authentication));
    }

    /** 更新当前用户的姓名、邮箱 */
    @PutMapping("/profile")
    public Map<String, Object> updateProfile(@RequestBody Map<String, String> userData, Authentication authentication) {
        User user = currentUser(authentication);
        if (userData.containsKey("name")) {
            user.setName(userData.get("name"));
        }
        if (userData.containsKey("email")) {
            user.setEmail(userData.get("email"));
        }
        return userResponse("个人信息已更新", userService.save(user));
    }

    /** 修改当前用户的密码，需要提供旧密码 */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> passwordData, Authentication authentication) {
        String oldPassword = passwordData.get("oldPassword");
        String newPassword = passwordData.get("newPassword");
        if (oldPassword == null || oldPassword.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            throw ApiException.badRequest("旧密码和新密码不能为空");
        }
        if (newPassword.length() < 8) {
            throw ApiException.badRequest("新密码至少需要 8 位");
        }
        if (!userService.changePassword(authentication.getName(), oldPassword, newPassword)) {
            throw ApiException.badRequest("旧密码不正确");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("message", "密码修改成功");
        return body;
    }

    private User currentUser(Authentication authentication) {
        // 会话里的用户被删除时按未登录处理
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "请先登录"));
    }

    private static Map<String, Object> userResponse(String message, User user) {
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
        return body;
    }
}
