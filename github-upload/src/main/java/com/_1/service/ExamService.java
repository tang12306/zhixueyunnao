package com._1.service;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Question;
import com._1.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ExamService {
    List<Exam> findAll();
    Page<Exam> findAll(Pageable pageable);
    Optional<Exam> findById(Long id);
    List<Exam> findBySubject(Subject subject);
    List<Exam> findByTargetClass(ClassEntity classEntity);
    List<Exam> findByExamDateBetween(Date startDate, Date endDate);
    Page<Exam> search(String keyword, Pageable pageable);
    Exam save(Exam exam);
    void deleteById(Long id);
    void addQuestionToExam(Long examId, Long questionId);
    void removeQuestionFromExam(Long examId, Long questionId);
    void addClassToExam(Long examId, Long classId);
    void removeClassFromExam(Long examId, Long classId);
    // 自动出题相关方法
    Exam generateExam(Long subjectId, String examType, List<Long> chapterIds, 
                     int choiceCount, int fillBlankCount, int trueFalseCount, int shortAnswerCount);
} 