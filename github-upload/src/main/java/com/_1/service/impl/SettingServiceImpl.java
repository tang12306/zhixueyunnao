package com._1.service.impl;

import com._1.entity.Setting;
import com._1.repository.SettingRepository;
import com._1.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SettingServiceImpl implements SettingService {

    private static final Logger logger = LoggerFactory.getLogger(SettingServiceImpl.class);
    private final SettingRepository settingRepository;

    @Autowired
    public SettingServiceImpl(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public List<Setting> getAllEditableSettings() {
        return settingRepository.findByIsEditable(true);
    }

    @Override
    public Optional<Setting> getSettingByKey(String key) {
        return settingRepository.findById(key);
    }

    @Override
    @Transactional
    public Setting saveSetting(Setting setting) {
        return settingRepository.save(setting);
    }

    @Override
    @Transactional
    public Setting updateSetting(String key, String value) {
        Setting setting = settingRepository.findById(key)
                .orElseThrow(() -> new RuntimeException("Setting not found with key: " + key));
        if (!setting.isEditable()) {
            throw new RuntimeException("Setting with key '" + key + "' is not editable.");
        }
        setting.setValue(value);
        return settingRepository.save(setting);
    }

    @Override
    @Transactional
    public void initializeDefaultSettings() {
        logger.info("Initializing default system settings...");
        createSettingIfNotExists("DEEPSEEK_API_KEY", "", "DeepSeek API密钥", "用于AI出题功能的DeepSeek平台API密钥。", "AI服务", true);
        createSettingIfNotExists("SITE_NAME", "教务题库管理系统", "系统名称", "显示在浏览器标题和系统中的名称。", "通用", true);
        createSettingIfNotExists("DEFAULT_PAGE_SIZE", "20", "默认分页大小", "表格中每页显示的条目数。", "显示", true);
        // Add more default settings as needed
        logger.info("Default system settings initialization complete.");
    }

    private void createSettingIfNotExists(String key, String value, String name, String description, String category, boolean isEditable) {
        if (settingRepository.findById(key).isEmpty()) {
            Setting setting = new Setting(key, value, name, description, category, isEditable);
            settingRepository.save(setting);
            logger.info("Created default setting: {}", key);
        }
    }
} 