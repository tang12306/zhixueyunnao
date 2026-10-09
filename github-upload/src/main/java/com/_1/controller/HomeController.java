package com._1.controller;

import com._1.entity.User; 
import com._1.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class HomeController {

    private final UserService userService;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String viewName = "index"; // 默认视图

        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            Object principal = authentication.getPrincipal();
            String username = null;

            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            } else if (principal instanceof String) {
                // 不太可能发生，因为我们检查了 !(authentication.getPrincipal() instanceof String)
                // 但作为安全措施
                username = (String) principal;
            }

            if (username != null) {
                Optional<User> userOptional = userService.findByUsername(username); 
                if (userOptional.isPresent()) {
                    User customUser = userOptional.get();
                    model.addAttribute("currentUser", customUser); 
                    // 根据角色确定视图或进一步操作，但CustomAuthenticationSuccessHandler会处理主要重定向
                    // 这里主要是为首页准备模型数据
                    // 例如，如果教师也看index，但有特定内容
                    if ("TEACHER".equals(customUser.getRole())) {
                        // model.addAttribute("isTeacher", true);
                    }
                } else {
                    // 用户已认证但在数据库中找不到 (异常情况)
                    // log.warn("Authenticated user '{}' not found in database.", username);
                }
            }
        }
        return viewName; // 返回主页模板名，例如 index.html
    }
} 