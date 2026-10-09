package com._1.service.impl;

import com._1.entity.Setting;
import com._1.repository.SettingRepository;
import com._1.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Service
public class SettingServiceImpl implements SettingService {

    private static final Logger logger = LoggerFactory.getLogger(SettingServiceImpl.class);

    /**
     * 已停用的设置项。DeepSeek 密钥只从环境变量 DEEPSEEK_API_KEY 读取，
     * 旧版本在数据库里建的这一行从来没被用过，现在既不展示也不允许修改。
     */
    private static final Set<String> RETIRED_KEYS = Set.of("DEEPSEEK_API_KEY");

    private final SettingRepository settingRepository;

    public SettingServiceImpl(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public List<Setting> getAllEditableSettings() {
        return settingRepository.findByIsEditable(true).stream()
                .filter(setting -> !RETIRED_KEYS.contains(setting.getKey()))
                .toList();
    }

    @Override
    public Optional<Setting> getSettingByKey(String key) {
        if (RETIRED_KEYS.contains(key)) {
            return Optional.empty();
        }
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
        Setting setting = getSettingByKey(key)
                .orElseThrow(() -> new NoSuchElementException("Setting not found with key: " + key));
        if (!setting.isEditable()) {
            throw new IllegalStateException("Setting with key '" + key + "' is not editable.");
        }
        setting.setValue(value);
        return settingRepository.save(setting);
    }

    @Override
    @Transactional
    public void initializeDefaultSettings() {
        logger.info("Initializing default system settings...");
        createSettingIfNotExists("SITE_NAME", "教务题库管理系统", "系统名称", "显示在浏览器标题和系统中的名称。", "通用", true);
        createSettingIfNotExists("DEFAULT_PAGE_SIZE", "20", "默认分页大小", "表格中每页显示的条目数。", "显示", true);
        // Add more default settings as needed
        settingRepository.findById("DEEPSEEK_API_KEY")
                .filter(setting -> setting.getValue() != null && !setting.getValue().isBlank())
                .ifPresent(setting -> logger.warn("数据库 settings 表中的 DEEPSEEK_API_KEY 不会被使用，"
                        + "密钥请配置在 .env 的 DEEPSEEK_API_KEY 中，并删除数据库里的这一行。"));
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
