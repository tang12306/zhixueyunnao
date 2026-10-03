package com._1.repository;

import com._1.entity.College;
import com._1.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegeRepository extends JpaRepository<College, Long> {
    List<College> findBySchool(School school);
    boolean existsByNameAndSchool(String name, School school);
    Optional<College> findByNameAndSchool(String name, School school);
    List<College> findBySchoolId(Long schoolId);
} 