package com._1.init;

import com._1.core.common.Roles;
import com._1.entity.School;
import com._1.entity.User;
import com._1.repository.SchoolRepository;
import com._1.service.UserService;
import com._1.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.Optional;

/**
 * 每次启动都会执行的基础数据初始化：学校、系统设置、首个管理员。
 * 演示账号只在 dev 环境创建，见 {@link DemoDataInitializer}。
 */
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final int MIN_ADMIN_PASSWORD_LENGTH = 8;

    private final UserService userService;
    private final SchoolRepository schoolRepository;
    private final SettingService settingService;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(UserService userService, SchoolRepository schoolRepository, SettingService settingService,
                           @Value("${app.bootstrap-admin.username:}") String adminUsername,
                           @Value("${app.bootstrap-admin.password:}") String adminPassword) {
        this.userService = userService;
        this.schoolRepository = schoolRepository;
        this.settingService = settingService;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting data initialization...");

        // 初始化学校
        initializeSchool();

        // 初始化系统设置
        settingService.initializeDefaultSettings();

        // 没有管理员时，用环境变量创建第一个
        initializeAdmin();

        logger.info("Data initialization process finished.");
    }

    private void initializeAdmin() {
        if (userService.countByRole(Roles.ADMIN) > 0) {
            return;
        }
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            logger.warn("系统中还没有管理员账号。请在 .env 中设置 APP_ADMIN_USERNAME 和 APP_ADMIN_PASSWORD 后重启。");
            return;
        }
        if (adminPassword.length() < MIN_ADMIN_PASSWORD_LENGTH) {
            logger.error("APP_ADMIN_PASSWORD 至少需要 {} 位，未创建管理员账号。", MIN_ADMIN_PASSWORD_LENGTH);
            return;
        }
        Optional<User> existing = userService.findByUsername(adminUsername);
        if (existing.isPresent()) {
            logger.error("用户名 {} 已被其他账号占用，未创建管理员账号。请换一个 APP_ADMIN_USERNAME。", adminUsername);
            return;
        }
        userService.saveUser(adminUsername, "系统管理员", adminPassword, Roles.ADMIN);
        logger.info("已创建管理员账号 {}。", adminUsername);
    }

    private void initializeSchool() {
        String schoolName = "江苏师范大学";
        Optional<School> existingSchool = schoolRepository.findByName(schoolName);
        if (existingSchool.isEmpty()) {
            School school = new School();
            school.setName(schoolName);
            schoolRepository.save(school);
            logger.info("Created default school: {}", schoolName);
        } else {
            logger.info("School '{}' already exists.", schoolName);
        }
    }
}
