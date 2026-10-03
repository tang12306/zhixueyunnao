package com._1.repository;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findBySubject(Subject subject);
    
    @Query("SELECT e FROM Exam e JOIN e.targetClasses c WHERE c = :classEntity")
    List<Exam> findByTargetClass(@Param("classEntity") ClassEntity classEntity);
    
    @Query("SELECT e FROM Exam e WHERE e.examDate >= :startDate AND e.examDate <= :endDate")
    List<Exam> findByExamDateBetween(@Param("startDate") Date startDate, @Param("endDate") Date endDate);
    
    @Query("SELECT e FROM Exam e WHERE " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.examType) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Exam> search(@Param("keyword") String keyword, Pageable pageable);
} 