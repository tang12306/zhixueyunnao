package com.example.service.impl;

import com.example.entity.Student;
import com.example.repository.StudentRepository;
import com.example.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean validateStudent(String studentId, String password) {
        Student student = studentRepository.findByStudentId(studentId);
        if (student != null) {
            return passwordEncoder.matches(password, student.getPassword());
        }
        return false;
    }
} 