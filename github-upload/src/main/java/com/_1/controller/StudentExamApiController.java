package com._1.controller;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.User;
import com._1.service.ExamService;
import com._1.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生考试相关的REST API控制器 - 已禁用
 * 只保留教师端功能
 *
 * 原始代码已注释，如需恢复学生端功能请取消注释
 */

// 学生考试API控制器已禁用 - 整个类已注释
/*
@RestController
@RequestMapping("/student/api")
public class StudentExamApiController {

    private static final Logger logger = LoggerFactory.getLogger(StudentExamApiController.class);

    @Autowired
    private ExamService examService;

    @Autowired
    private UserService userService;

    @GetMapping("/exams")
    public ResponseEntity<?> getStudentExams(Authentication authentication) {
        // 学生考试列表功能已禁用
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "学生端功能已禁用，请使用教师端");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exams/{examId}")
    public ResponseEntity<?> getExamDetails(@PathVariable Long examId, Authentication authentication) {
        // 学生考试详情功能已禁用
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "学生端功能已禁用，请使用教师端");
        return ResponseEntity.ok(response);
    }
}
*/