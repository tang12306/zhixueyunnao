package com._1.controller;

import com._1.core.exception.ApiException;
import com._1.entity.Setting;
import com._1.service.SettingService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/settings")
public class SettingApiController {

    private final SettingService settingService;

    public SettingApiController(SettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public List<Setting> getAllEditableSettings() {
        List<Setting> settings = settingService.getAllEditableSettings();
        // 还没有设置数据时先写入默认设置
        if (settings.isEmpty()) {
            settingService.initializeDefaultSettings();
            settings = settingService.getAllEditableSettings();
        }
        return settings;
    }

    @GetMapping("/{key}")
    public Setting getSettingByKey(@PathVariable String key) {
        return settingService.getSettingByKey(key).orElseThrow(() -> ApiException.notFound("设置项不存在"));
    }

    // 只修改已有设置项，默认设置由 initializeDefaultSettings 写入
    @PutMapping("/{key}")
    public Setting updateSetting(@PathVariable String key, @RequestBody Map<String, String> payload) {
        String value = payload.get("value");
        if (value == null) {
            throw ApiException.badRequest("请填写设置值");
        }
        try {
            return settingService.updateSetting(key, value);
        } catch (NoSuchElementException e) {
            throw ApiException.notFound("设置项不存在");
        } catch (IllegalStateException e) {
            throw new ApiException(HttpStatus.FORBIDDEN, "该设置项不允许修改");
        }
    }
}
