package com._1.repository;

import com._1.entity.ReflectionReport;
import com._1.entity.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ReflectionReportRepository extends JpaRepository<ReflectionReport, Long> {
    Optional<ReflectionReport> findByScore(Score score);
    List<ReflectionReport> findByUserId(Long userId);
} 