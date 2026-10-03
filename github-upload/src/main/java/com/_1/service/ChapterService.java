package com._1.service;

import com._1.entity.Chapter;
import com._1.entity.Subject;
import java.util.List;
import java.util.Optional;

public interface ChapterService {
    List<Chapter> findAll();
    List<Chapter> findBySubject(Subject subject);
    Optional<Chapter> findById(Long id);
    Chapter save(Chapter chapter);
    void deleteById(Long id);
    boolean existsByNameAndSubject(String name, Subject subject);
    Optional<Chapter> findByNameAndSubject(String name, Subject subject);
} 