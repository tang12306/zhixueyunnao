package com.example.service;

public interface StudentService {
    /**
     * 验证学生学号是否存在
     * @param studentId 学号
     * @param password 密码
     * @return 如果学号存在返回true，否则返回false
     */
    boolean validateStudent(String studentId, String password);
} 