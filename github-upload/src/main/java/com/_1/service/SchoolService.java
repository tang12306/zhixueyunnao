package com._1.service;

import com._1.entity.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface SchoolService {
    List<School> findAll();
    Page<School> findAll(Pageable pageable);
    Optional<School> findById(Long id);
    Optional<School> findByName(String name);
    School save(School school);
    void deleteById(Long id);
    boolean existsByName(String name);
} 