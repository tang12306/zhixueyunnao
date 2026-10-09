package com._1.controller;

import com._1.core.common.Roles;
import com._1.core.exception.ApiException;
import com._1.dto.StudentDTO;
import com._1.entity.ClassEntity;
import com._1.entity.User;
import com._1.service.ClassService;
import com._1.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// 学生API控制器 - 教师端学生管理功能
@RestController
@RequestMapping("/api/students")
public class StudentApiController {

    private static final Logger logger = LoggerFactory.getLogger(StudentApiController.class);

    private final UserService userService;
    private final ClassService classService;

    public StudentApiController(UserService userService, ClassService classService) {
        this.userService = userService;
        this.classService = classService;
    }

    // 获取学生列表（只查学生角色，返回不含密码的 DTO）
    @GetMapping
    public List<StudentDTO> getAllStudents() {
        return userService.findByRole(Roles.STUDENT).stream().map(StudentDTO::from).toList();
    }

    // 学生人数（控制台统计用，不必拉取整个列表）
    @GetMapping("/count")
    public Map<String, Long> countStudents() {
        return Map.of("count", userService.countByRole(Roles.STUDENT));
    }

    @GetMapping("/{id}")
    public StudentDTO getStudentById(@PathVariable Long id) {
        return StudentDTO.from(findStudent(id));
    }

    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(@RequestBody Map<String, Object> studentData) {
        String username = (String) studentData.get("username");
        if (username == null || username.isEmpty()) {
            throw ApiException.badRequest("学号不能为空");
        }
        if (userService.findByUsername(username).isPresent()) {
            throw ApiException.conflict("学号 '" + username + "' 已存在");
        }

        User student = new User();
        student.setUsername(username);
        student.setName((String) studentData.get("name"));
        student.setGender((String) studentData.get("gender"));
        student.setPhone((String) studentData.get("phone"));
        student.setEmail((String) studentData.get("email"));
        student.setRole(Roles.STUDENT);
        student.setEnabled(true);
        applyBirthday(student, studentData.get("birthday"));

        // 没填密码时以学号作为初始密码
        String password = (String) studentData.get("password");
        student.setPassword(userService.encodePassword(password != null && !password.isEmpty() ? password : username));

        ClassEntity clazz = resolveClass(studentData.get("clazz"));
        if (clazz != null) {
            student.setStudentClass(clazz);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(StudentDTO.from(userService.save(student)));
    }

    @PutMapping("/{id}")
    public StudentDTO updateStudent(@PathVariable Long id, @RequestBody Map<String, Object> studentData) {
        User existingStudent = findStudent(id);

        if (studentData.get("name") != null) {
            existingStudent.setName((String) studentData.get("name"));
        }
        if (studentData.get("username") != null) {
            String newUsername = (String) studentData.get("username");
            if (!newUsername.equals(existingStudent.getUsername()) && userService.findByUsername(newUsername).isPresent()) {
                throw ApiException.conflict("学号 '" + newUsername + "' 已存在");
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
        applyBirthday(existingStudent, studentData.get("birthday"));

        // 传了 clazz 才改班级；clazz 为空表示解除班级关联
        if (studentData.containsKey("clazz")) {
            existingStudent.setStudentClass(resolveClass(studentData.get("clazz")));
        }

        // 只有填写了新密码才修改
        String password = (String) studentData.get("password");
        if (password != null && !password.isEmpty()) {
            existingStudent.setPassword(userService.encodePassword(password));
        }
        return StudentDTO.from(userService.save(existingStudent));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        findStudent(id);
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // 学生表单的班级下拉框
    @GetMapping("/available-classes")
    public List<ClassEntity> getAllClasses() {
        return classService.findAll();
    }

    private User findStudent(Long id) {
        return userService.findById(id)
                .filter(user -> Roles.STUDENT.equals(user.getRole()))
                .orElseThrow(() -> ApiException.notFound("学生不存在"));
    }

    private static void applyBirthday(User student, Object birthday) {
        if (birthday == null) {
            return;
        }
        try {
            student.setBirthday(java.sql.Date.valueOf(birthday.toString()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("出生日期格式应为 yyyy-MM-dd");
        }
    }

    /** 前端发送的是 clazz: {id}；没有 id 时返回 null */
    private ClassEntity resolveClass(Object clazzData) {
        if (!(clazzData instanceof Map<?, ?> clazz) || clazz.get("id") == null) {
            return null;
        }
        Long classId;
        Object idObj = clazz.get("id");
        try {
            classId = idObj instanceof Number number ? number.longValue() : Long.parseLong(idObj.toString());
        } catch (NumberFormatException e) {
            logger.debug("Invalid class ID format: {}", idObj);
            throw ApiException.badRequest("班级参数不正确");
        }
        return classService.findById(classId).orElseThrow(() -> ApiException.badRequest("指定的班级不存在"));
    }
}
