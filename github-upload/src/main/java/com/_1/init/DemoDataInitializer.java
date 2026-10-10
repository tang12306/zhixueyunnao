package com._1.init;

import com._1.core.common.Roles;
import com._1.dto.StudentData;
import com._1.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地演示数据，只在 dev 环境创建。账号密码是公开的，生产环境不要开启 dev profile。
 * 在 {@link DataInitializer} 之后执行，.env 里配置的管理员和演示账号都会创建。
 */
@Component
@Profile("dev")
@Order(1)
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoDataInitializer.class);
    private final UserService userService;

    public DemoDataInitializer(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.warn("dev 环境：正在创建公开密码的演示账号，生产环境请勿开启 dev profile。");

        List<StudentData> studentsToProcess = new ArrayList<>();

        // 仅使用虚构演示数据，不包含真实学生姓名或学号
        for (int index = 1; index <= 58; index++) {
            String studentId = String.format("990000%03d", index);
            String studentName = String.format("示例学生%03d", index);
            studentsToProcess.add(new StudentData(studentId, studentName));
        }

        logger.info("Attempting to save {} student users.", studentsToProcess.size());
        for (StudentData data : studentsToProcess) {
            // 学号作为用户名，也作为初始密码，角色为STUDENT；saveUser 会跳过已存在的用户
            userService.saveUser(data.getId(), data.getName(), data.getId(), "STUDENT");
        }

        // 虚构教师与管理员账号，仅用于本地演示
        userService.saveUser("demo.teacher", "演示教师", "DemoTeacher123!", "TEACHER");
        userService.saveUser("demo.admin", "演示管理员", "DemoAdmin123!", Roles.ADMIN);

        logger.info("Demo data initialization finished.");
    }
}
