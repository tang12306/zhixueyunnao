package com._1.service;

import com._1.entity.College;
import com._1.entity.Major;
import com._1.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MajorService {
    List<Major> findAll();
    Page<Major> findAll(Pageable pageable);
    Optional<Major> findById(Long id);
    List<Major> findByCollege(College college);
    Optional<Major> findByNameAndCollege(String name, College college);
    List<Major> findBySubject(Subject subject);
    Major save(Major major);
    void deleteById(Long id);
    boolean existsByNameAndCollege(String name, College college);
    void addSubjectToMajor(Long majorId, Long subjectId);
    void removeSubjectFromMajor(Long majorId, Long subjectId);
    List<Major> findByCollegeId(Long collegeId);
    boolean existsByNameAndCollegeId(String name, Long collegeId);
    Optional<Major> findByNameAndCollegeId(String name, Long collegeId);
} 