package com._1.repository;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Score;
import com._1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByUser(User user);
    List<Score> findByExam(Exam exam);
    Optional<Score> findByUserAndExam(User user, Exam exam);
    
    @Query("SELECT s FROM Score s WHERE s.user.studentClass = :classEntity AND s.exam = :exam")
    List<Score> findByClassAndExam(@Param("classEntity") ClassEntity classEntity, @Param("exam") Exam exam);

    @Query("SELECT AVG(s.score) FROM Score s WHERE s.exam = :exam AND s.user.studentClass = :classEntity")
    Double findAverageScoreByExamAndClass(@Param("exam") Exam exam, @Param("classEntity") ClassEntity classEntity);

    @Query("SELECT MAX(s.score) FROM Score s WHERE s.exam = :exam AND s.user.studentClass = :classEntity")
    Double findMaxScoreByExamAndClass(@Param("exam") Exam exam, @Param("classEntity") ClassEntity classEntity);

    @Query("SELECT MIN(s.score) FROM Score s WHERE s.exam = :exam AND s.user.studentClass = :classEntity")
    Double findMinScoreByExamAndClass(@Param("exam") Exam exam, @Param("classEntity") ClassEntity classEntity);
} 