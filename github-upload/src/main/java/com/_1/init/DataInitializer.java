package com._1.init;

import com._1.dto.StudentData;
import com._1.entity.School;
import com._1.repository.SchoolRepository;
import com._1.service.UserService;
import com._1.service.SettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private final UserService userService;
    private final SchoolRepository schoolRepository;
    private final SettingService settingService;

    public DataInitializer(UserService userService, SchoolRepository schoolRepository, SettingService settingService) {
        this.userService = userService;
        this.schoolRepository = schoolRepository;
        this.settingService = settingService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting data initialization...");

        // 初始化学校
        initializeSchool();

        // 初始化系统设置
        settingService.initializeDefaultSettings();

        List<StudentData> studentsToProcess = new ArrayList<>();

        // 仅使用虚构演示数据，不包含真实学生姓名或学号
        for (int index = 1; index <= 58; index++) {
            String studentId = String.format("990000%03d", index);
            String studentName = String.format("示例学生%03d", index);
            studentsToProcess.add(new StudentData(studentId, studentName));
        }

        if (!studentsToProcess.isEmpty()) {
            logger.info("Attempting to save {} student users and entities.", studentsToProcess.size());
            for (StudentData data : studentsToProcess) {
                if (data.getId() == null || data.getName() == null) {
                    logger.warn("Skipping student data with null id (studentId) or name: {}", data);
                    continue;
                }
                
                // 1. 创建/获取 User 账户
                // 学号作为用户名，也作为初始密码，角色为STUDENT
                // saveUser 方法内部会检查用户是否已存在
                userService.saveUser(data.getId(), data.getName(), data.getId(), "STUDENT");
                // logger.info("Processed User: {}", data.getId()); // saveUser 内部已有日志

                // 2. 创建/获取 Student 实体 (REMOVED - Student entity is merged into User)
                // Optional<Student> existingStudent = studentService.findByStudentId(data.getId()); // data.getId() 是学号
                // if (existingStudent.isEmpty()) {
                //     Student studentEntity = new Student();
                //     studentEntity.setStudentId(data.getId()); // 设置学号
                //     studentEntity.setName(data.getName());    // 设置姓名
                //     studentService.save(studentEntity);
                //     logger.info("Saved new Student entity for studentId: {}", data.getId());
                // } else {
                //     // logger.info("Student entity for studentId: {} already exists. Skipping creation or update.", data.getId());
                // }
            }
            logger.info("Student data processing finished.");
        } else {
            logger.info("No student data to process.");
        }

        // 初始化虚构教师账号，仅用于本地演示
        logger.info("Attempting to save/update teacher: demo.teacher");
        userService.saveUser("demo.teacher", "演示教师", "DemoTeacher123!", "TEACHER");
        
        logger.info("Data initialization process finished.");
    }

    private void initializeSchool() {
        String schoolName = "江苏师范大学";
        Optional<School> existingSchool = schoolRepository.findByName(schoolName);
        if (existingSchool.isEmpty()) {
            School school = new School();
            school.setName(schoolName);
            // school.setAddress("默认地址"); // 可以根据需要设置其他默认值
            // school.setDescription("默认描述");
            schoolRepository.save(school);
            logger.info("Created default school: {}", schoolName);
        } else {
            logger.info("School '{}' already exists.", schoolName);
        }
    }
} 