package com._1.service.impl;

import com._1.entity.ReflectionReport;
import com._1.repository.ReflectionReportRepository;
import com._1.service.ReflectionReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReflectionReportServiceImpl implements ReflectionReportService {

    @Autowired
    private ReflectionReportRepository reflectionReportRepository;

    @Override
    public ReflectionReport save(ReflectionReport report) {
        return reflectionReportRepository.save(report);
    }

    @Override
    public Optional<ReflectionReport> findById(Long id) {
        return reflectionReportRepository.findById(id);
    }

    @Override
    public List<ReflectionReport> findByUserId(Long userId) {
        return reflectionReportRepository.findByUserId(userId);
    }
} 