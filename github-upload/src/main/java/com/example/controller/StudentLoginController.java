package com.example.controller;

import com.example.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student")
public class StudentLoginController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/login")
    public String loginPage() {
        return "student/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String studentId,
                        @RequestParam String password,
                       HttpSession session,
                       Model model) {
        try {
            // 验证学号和密码
            if (studentService.validateStudent(studentId, password)) {
                // 登录成功，将学号存入session
                session.setAttribute("studentId", studentId);
                return "redirect:/student/exams";
            } else {
                model.addAttribute("error", "学号或密码错误，请重试");
                return "student/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "登录过程中发生错误，请稍后重试");
            return "student/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("studentId");
        return "redirect:/student/login";
    }
} 