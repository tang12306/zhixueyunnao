package com._1.controller;

import com._1.entity.Setting;
import com._1.service.SettingService;
import com._1.service.UserService;
import com._1.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/settings")
public class SettingsController {
    @Autowired
    private SettingService settingService;

    @Autowired
    private UserService userService;

    @GetMapping
    public String settings(Model model) {
        Optional<Setting> maxImportSetting = settingService.getSettingByKey("maxImportCount");
        String maxImport = maxImportSetting.isPresent() ? maxImportSetting.get().getValue() : "1000";
        model.addAttribute("maxImportCount", maxImport);
        model.addAttribute("developers", "项目开发团队");
        model.addAttribute("users", userService.findAll());
        return "settings";
    }

    @PostMapping("/save")
    public String save(@RequestParam String maxImportCount) {
        Optional<Setting> settingOpt = settingService.getSettingByKey("maxImportCount");
        Setting setting = settingOpt.orElse(new Setting());
        setting.setKey("maxImportCount");
        setting.setValue(maxImportCount);
        settingService.saveSetting(setting);
        return "redirect:/settings";
    }

    @PostMapping("/addUser")
    public String addUser(@RequestParam String username, @RequestParam String password, @RequestParam String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        userService.save(user);
        return "redirect:/settings";
    }

    @PostMapping("/deleteUser/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return "redirect:/settings";
    }

    @PostMapping("/resetPassword/{id}")
    public String resetPassword(@PathVariable Long id, @RequestParam String newPassword) {
        userService.resetPassword(id, newPassword);
        return "redirect:/settings";
    }
    
    @PostMapping("/updateUser")
    public String updateUser(@RequestParam Long userId, @RequestParam String username, @RequestParam String role) {
        User user = userService.findById(userId).orElse(null);
        if (user != null) {
            user.setUsername(username);
            user.setRole(role);
            userService.save(user);
        }
        return "redirect:/settings";
    }
    
    @PostMapping("/changePassword")
    @ResponseBody
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        String currentPassword = request.get("currentPassword");
        String newPassword = request.get("newPassword");
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        
        boolean success = userService.changePassword(currentUsername, currentPassword, newPassword);
        
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/resetPassword")
    public String resetPasswordWithConfirm(@RequestParam Long userId, 
                                          @RequestParam String newPassword, 
                                          @RequestParam String confirmPassword) {
        if (newPassword.equals(confirmPassword)) {
            userService.resetPassword(userId, newPassword);
        }
        return "redirect:/settings";
    }
} 