package com._1.service;

import com._1.entity.ReflectionReport;

import java.util.List;
import java.util.Optional;

public interface ReflectionReportService {
    ReflectionReport save(ReflectionReport report);
    Optional<ReflectionReport> findById(Long id);
    List<ReflectionReport> findByUserId(Long userId);
} 