package com._1.service;

import com._1.entity.Subject;
import java.util.List;
import java.util.Optional;

public interface SubjectService {
    List<Subject> findAll();
    Optional<Subject> findById(Long id);
    Subject save(Subject subject);
    void deleteById(Long id);
    boolean existsByName(String name);
    List<Subject> findAllById(List<Long> ids);
    Optional<Subject> findByName(String name);
} 