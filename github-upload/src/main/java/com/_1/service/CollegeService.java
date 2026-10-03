package com._1.service;

import com._1.entity.College;
import com._1.entity.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CollegeService {
    List<College> findAll();
    Page<College> findAll(Pageable pageable);
    Optional<College> findById(Long id);
    List<College> findBySchool(School school);
    Optional<College> findByNameAndSchool(String name, School school);
    College save(College college);
    void deleteById(Long id);
    boolean existsByNameAndSchool(String name, School school);
    List<College> findBySchoolId(Long schoolId);
    Optional<College> findByNameAndSchool(String name, Long schoolId);
    boolean existsByNameAndSchool(String name, Long schoolId);
} 