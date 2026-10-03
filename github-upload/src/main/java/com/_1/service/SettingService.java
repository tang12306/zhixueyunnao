package com._1.service;

import com._1.entity.Setting;

import java.util.List;
import java.util.Optional;

public interface SettingService {
    List<Setting> getAllEditableSettings();
    Optional<Setting> getSettingByKey(String key);
    Setting saveSetting(Setting setting);
    Setting updateSetting(String key, String value);
    void initializeDefaultSettings();
} 