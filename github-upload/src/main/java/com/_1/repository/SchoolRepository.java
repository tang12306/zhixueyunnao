package com._1.repository;

import com._1.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchoolRepository extends JpaRepository<School, Long> {
    boolean existsByName(String name);
    Optional<School> findByName(String name);
} 