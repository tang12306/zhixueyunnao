package com._1.service;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Score;
import com._1.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ScoreService {
    List<Score> findAll();
    Page<Score> findAll(Pageable pageable);
    Optional<Score> findById(Long id);
    List<Score> findByUser(User user);
    List<Score> findByExam(Exam exam);
    Optional<Score> findByUserAndExam(User user, Exam exam);
    List<Score> findByClassAndExam(ClassEntity classEntity, Exam exam);
    Score save(Score score);
    void deleteById(Long id);
    
    // 成绩统计分析方法
    Double findAverageScoreByExamAndClass(Exam exam, ClassEntity classEntity);
    Double findMaxScoreByExamAndClass(Exam exam, ClassEntity classEntity);
    Double findMinScoreByExamAndClass(Exam exam, ClassEntity classEntity);
    
    // 成绩导入方法
    List<Score> importScores(MultipartFile file, Long examId);
    List<Score> importScoresFromExcel(MultipartFile file, Exam exam, ClassEntity classEntity) throws IOException;
    
    // 成绩分析方法
    Map<String, Object> analyzeScoresByExam(Long examId);
    Map<String, Object> analyzeStudentScoresTrend(Long studentId, Long subjectId);
    Map<String, Object> compareClassesAverageScores(List<Long> classIds, Long examId);
    
    // 班级成绩分析方法
    Double getClassAverageScore(Long classId, Long examId);
    Map<String, Integer> getClassScoreDistribution(Long classId, Long examId);
} 