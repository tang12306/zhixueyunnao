package com._1.service;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Major;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ClassService {
    List<ClassEntity> findAll();
    Page<ClassEntity> findAll(Pageable pageable);
    Optional<ClassEntity> findById(Long id);
    List<ClassEntity> findAllById(Iterable<Long> ids);
    List<ClassEntity> findByMajor(Major major);
    List<ClassEntity> findByMajorAndGrade(Major major, Integer grade);
    Optional<ClassEntity> findByNameAndMajorAndGrade(String name, Major major, Integer grade);
    List<ClassEntity> findByExam(Exam exam);
    ClassEntity save(ClassEntity classEntity);
    void deleteById(Long id);
    void addExamToClass(Long classId, Long examId);
    void removeExamFromClass(Long classId, Long examId);
    List<ClassEntity> findByMajorId(Long majorId);
    boolean existsByNameAndMajorId(String name, Long majorId);

    // 新增：检查班级下的学生数量
    long countStudentsInClass(Long classId);

    // 新增：检查班级是否可以删除（无学生）
    boolean canDeleteClass(Long classId);
}