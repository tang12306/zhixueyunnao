package com._1.repository;

import com._1.entity.ClassEntity;
import com._1.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassEntityRepository extends JpaRepository<ClassEntity, Long> {
    List<ClassEntity> findByMajor(Major major);
    List<ClassEntity> findByMajorAndGrade(Major major, Integer grade);
    Optional<ClassEntity> findByNameAndMajorAndGrade(String name, Major major, Integer grade);
    
    @Query("SELECT c FROM ClassEntity c JOIN c.exams e WHERE e.id = :examId")
    List<ClassEntity> findByExamId(@Param("examId") Long examId);

    List<ClassEntity> findByMajorId(Long majorId);
    boolean existsByNameAndMajorId(String name, Long majorId);
} 