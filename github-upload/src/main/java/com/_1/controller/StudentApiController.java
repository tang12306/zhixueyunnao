package com._1.controller;

import com._1.entity.ClassEntity;
import com._1.entity.User;
// import com._1.enums.UserRole; // UserRole enum is not used, role is String
import com._1.service.ClassService;
import com._1.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// 学生API控制器 - 教师端学生管理功能
@RestController
@RequestMapping("/api/students")
public class StudentApiController {

    private static final Logger logger = LoggerFactory.getLogger(StudentApiController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private ClassService classService;

    @Autowired
    private PasswordEncoder passwordEncoder; // 注入 PasswordEncoder

    // 获取所有（或特定角色）的学生列表
    @GetMapping
    public ResponseEntity<List<User>> getAllStudents() {
        try {
            List<User> students = userService.findAll().stream()
                    .filter(user -> "STUDENT".equals(user.getRole()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            logger.error("Error fetching all students", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 获取指定ID的学生信息
    @GetMapping("/{id}")
    public ResponseEntity<User> getStudentById(@PathVariable Long id) {
        try {
            Optional<User> studentOptional = userService.findById(id);
            if (studentOptional.isPresent() && "STUDENT".equals(studentOptional.get().getRole())) {
                return ResponseEntity.ok(studentOptional.get());
            } else {
                return ResponseEntity.notFound().build(); // 未找到或角色不是学生
            }
        } catch (Exception e) {
            logger.error("Error fetching student with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 创建新学生
    @PostMapping
    public ResponseEntity<?> createStudent(@RequestBody Map<String, Object> studentData) {
        try {
            // 检查用户名（学号）是否已存在
            String username = (String) studentData.get("username");
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "学号不能为空。"));
            }
            Optional<User> existingUser = userService.findByUsername(username);
            if (existingUser.isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "学号 '" + username + "' 已存在。"));
            }

            // 创建新的学生对象
            User student = new User();
            student.setUsername(username);
            student.setName((String) studentData.get("name"));
            student.setGender((String) studentData.get("gender"));
            student.setPhone((String) studentData.get("phone"));
            student.setEmail((String) studentData.get("email"));
            student.setRole("STUDENT");
            student.setEnabled(true);

            // 处理生日字段
            if (studentData.get("birthday") != null) {
                String birthdayStr = studentData.get("birthday").toString();
                try {
                    java.sql.Date birthday = java.sql.Date.valueOf(birthdayStr);
                    student.setBirthday(birthday);
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid birthday format: {}", birthdayStr);
                }
            }

            // 处理密码
            String password = (String) studentData.get("password");
            if (password != null && !password.isEmpty()) {
                student.setPassword(password);
            } else {
                // 如果没有提供密码，使用学号作为默认密码
                student.setPassword(username);
            }

            // 处理班级信息 - 前端发送的是 clazz 字段
            @SuppressWarnings("unchecked")
            Map<String, Object> clazzData = (Map<String, Object>) studentData.get("clazz");
            if (clazzData != null && clazzData.get("id") != null) {
                Long classId = null;
                Object idObj = clazzData.get("id");
                if (idObj instanceof Number) {
                    classId = ((Number) idObj).longValue();
                } else if (idObj instanceof String) {
                    try {
                        classId = Long.parseLong((String) idObj);
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid class ID format: {}", idObj);
                    }
                }

                if (classId != null) {
                    Optional<ClassEntity> classOptional = classService.findById(classId);
                    if (classOptional.isPresent()) {
                        student.setStudentClass(classOptional.get());
                    } else {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "指定的班级不存在。"));
                    }
                }
            }

            // userService.save 会处理密码加密
            User createdStudent = userService.save(student);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStudent);
        } catch (Exception e) {
            logger.error("Error creating student", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "创建学生时发生未知错误: " + e.getMessage()));
        }
    }

    // 更新学生信息
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStudent(@PathVariable Long id, @RequestBody Map<String, Object> studentData) {
        try {
            Optional<User> studentOptional = userService.findById(id);
            if (studentOptional.isPresent() && "STUDENT".equals(studentOptional.get().getRole())) {
                User existingStudent = studentOptional.get();

                // 更新允许修改的字段
                if (studentData.get("name") != null) {
                    existingStudent.setName((String) studentData.get("name"));
                }
                if (studentData.get("username") != null) {
                    String newUsername = (String) studentData.get("username");
                    // 检查新学号是否与其他用户冲突
                    if (!newUsername.equals(existingStudent.getUsername())) {
                        Optional<User> existingUser = userService.findByUsername(newUsername);
                        if (existingUser.isPresent()) {
                            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("success", false, "message", "学号 '" + newUsername + "' 已存在。"));
                        }
                    }
                    existingStudent.setUsername(newUsername);
                }
                if (studentData.get("gender") != null) {
                    existingStudent.setGender((String) studentData.get("gender"));
                }
                if (studentData.get("phone") != null) {
                    existingStudent.setPhone((String) studentData.get("phone"));
                }
                if (studentData.get("email") != null) {
                    existingStudent.setEmail((String) studentData.get("email"));
                }

                // 处理生日字段
                if (studentData.get("birthday") != null) {
                    String birthdayStr = studentData.get("birthday").toString();
                    try {
                        java.sql.Date birthday = java.sql.Date.valueOf(birthdayStr);
                        existingStudent.setBirthday(birthday);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid birthday format: {}", birthdayStr);
                    }
                }

                // 处理班级信息 - 前端发送的是 clazz 字段
                if (studentData.containsKey("clazz")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> clazzData = (Map<String, Object>) studentData.get("clazz");
                    if (clazzData != null && clazzData.get("id") != null) {
                        Long classId = null;
                        Object idObj = clazzData.get("id");
                        if (idObj instanceof Number) {
                            classId = ((Number) idObj).longValue();
                        } else if (idObj instanceof String) {
                            try {
                                classId = Long.parseLong((String) idObj);
                            } catch (NumberFormatException e) {
                                logger.warn("Invalid class ID format: {}", idObj);
                            }
                        }

                        if (classId != null) {
                            Optional<ClassEntity> classOptional = classService.findById(classId);
                            if (classOptional.isPresent()) {
                                existingStudent.setStudentClass(classOptional.get());
                            } else {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "指定的班级不存在。"));
                            }
                        }
                    } else {
                        existingStudent.setStudentClass(null); // 允许解除班级关联
                    }
                }

                // 如果传入了新密码且不为空，则更新密码。userService.save 会处理加密
                String password = (String) studentData.get("password");
                if (password != null && !password.isEmpty()) {
                    existingStudent.setPassword(password);
                }

                User updatedStudent = userService.save(existingStudent);
                return ResponseEntity.ok(updatedStudent);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error updating student with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "更新学生时发生错误: " + e.getMessage()));
        }
    }

    // 删除学生
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        try {
            Optional<User> studentOptional = userService.findById(id);
            if (studentOptional.isPresent() && "STUDENT".equals(studentOptional.get().getRole())) {
                userService.deleteById(id);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting student with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // API 端点，用于获取所有班级列表 (给学生表单下拉框使用)
    @GetMapping("/available-classes")
    public ResponseEntity<List<ClassEntity>> getAllClasses() {
        try {
            List<ClassEntity> classes = classService.findAll();
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            logger.error("Error fetching all classes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}