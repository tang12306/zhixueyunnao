package com._1.service.impl;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Major;
import com._1.repository.ClassEntityRepository;
import com._1.repository.ExamRepository;
import com._1.repository.MajorRepository;
import com._1.repository.UserRepository;
import com._1.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClassServiceImpl implements ClassService {

    @Autowired
    private ClassEntityRepository classEntityRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private MajorRepository majorRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<ClassEntity> findAll() {
        return classEntityRepository.findAll();
    }
    
    @Override
    public Page<ClassEntity> findAll(Pageable pageable) {
        return classEntityRepository.findAll(pageable);
    }

    @Override
    public Optional<ClassEntity> findById(Long id) {
        return classEntityRepository.findById(id);
    }
    
    @Override
    public List<ClassEntity> findAllById(Iterable<Long> ids) {
        return classEntityRepository.findAllById(ids);
    }

    @Override
    public List<ClassEntity> findByMajor(Major major) {
        return classEntityRepository.findByMajor(major);
    }

    @Override
    public List<ClassEntity> findByMajorAndGrade(Major major, Integer grade) {
        return classEntityRepository.findByMajorAndGrade(major, grade);
    }

    @Override
    public Optional<ClassEntity> findByNameAndMajorAndGrade(String name, Major major, Integer grade) {
        return classEntityRepository.findByNameAndMajorAndGrade(name, major, grade);
    }
    
    @Override
    public List<ClassEntity> findByExam(Exam exam) {
        if (exam == null || exam.getId() == null) return List.of();
        return classEntityRepository.findByExamId(exam.getId());
    }

    @Override
    public ClassEntity save(ClassEntity classEntity) {
        return classEntityRepository.save(classEntity);
    }

    @Override
    public void deleteById(Long id) {
        classEntityRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public void addExamToClass(Long classId, Long examId) {
        ClassEntity classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + classId));
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId));
        
        if (!exam.getTargetClasses().contains(classEntity)) {
            exam.getTargetClasses().add(classEntity);
            examRepository.save(exam);
        }
    }
    
    @Override
    @Transactional
    public void removeExamFromClass(Long classId, Long examId) {
        ClassEntity classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new RuntimeException("Class not found with id: " + classId));
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found with id: " + examId));

        if (exam.getTargetClasses().contains(classEntity)) {
            exam.getTargetClasses().remove(classEntity);
            examRepository.save(exam);
        }
    }

    @Override
    public List<ClassEntity> findByMajorId(Long majorId) {
        return classEntityRepository.findByMajorId(majorId);
    }

    @Override
    public boolean existsByNameAndMajorId(String name, Long majorId) {
        return classEntityRepository.existsByNameAndMajorId(name, majorId);
    }

    @Override
    public long countStudentsInClass(Long classId) {
        return userRepository.countByStudentClassIdAndRole(classId, "STUDENT");
    }

    @Override
    public boolean canDeleteClass(Long classId) {
        return countStudentsInClass(classId) == 0;
    }
}