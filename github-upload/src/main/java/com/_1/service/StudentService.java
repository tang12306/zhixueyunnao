/*
package com._1.service;

import com._1.entity.ClassEntity;
import com._1.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface StudentService {
    Student saveStudent(Student student);
    Optional<Student> findById(Long id);
    Optional<Student> findByStudentId(String studentId);
    List<Student> findAllStudents();
    Page<Student> findAllStudents(Pageable pageable);
    void deleteStudent(Long id);
    List<Student> findByClass(ClassEntity classEntity);
    Page<Student> findByClass(ClassEntity classEntity, Pageable pageable);
    Page<Student> searchStudents(String keyword, Pageable pageable);
    Student updateStudent(Student student);
}
*/ 