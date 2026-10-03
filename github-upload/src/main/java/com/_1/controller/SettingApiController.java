package com._1.controller;

import com._1.entity.Setting;
import com._1.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingApiController {

    private final SettingService settingService;

    @Autowired
    public SettingApiController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public ResponseEntity<List<Setting>> getAllEditableSettings() {
        List<Setting> settings = settingService.getAllEditableSettings();

        // 如果没有设置数据，初始化默认设置
        if (settings.isEmpty()) {
            settingService.initializeDefaultSettings();
            settings = settingService.getAllEditableSettings();
        }

        return ResponseEntity.ok(settings);
    }

    @GetMapping("/{key}")
    public ResponseEntity<Setting> getSettingByKey(@PathVariable String key) {
        return settingService.getSettingByKey(key)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // In this new structure, direct creation might be less common, 
    // defaults are handled by initializeDefaultSettings.
    // This endpoint could be for admins to add new, unplanned settings if needed.
    // For simplicity, we'll focus on updating existing settings for now.
    /*
    @PostMapping
    public ResponseEntity<Setting> createSetting(@RequestBody Setting setting) {
        Setting savedSetting = settingService.saveSetting(setting);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSetting);
    }
    */

    @PutMapping("/{key}")
    public ResponseEntity<?> updateSetting(@PathVariable String key, @RequestBody Map<String, String> payload) {
        String value = payload.get("value");
        if (value == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Value is required in payload."));
        }
        try {
            Setting updatedSetting = settingService.updateSetting(key, value);
            return ResponseEntity.ok(updatedSetting);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
} 