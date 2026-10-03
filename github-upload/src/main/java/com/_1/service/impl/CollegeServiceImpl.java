package com._1.service.impl;

import com._1.entity.College;
import com._1.entity.School;
import com._1.repository.CollegeRepository;
import com._1.repository.SchoolRepository;
import com._1.service.CollegeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CollegeServiceImpl implements CollegeService {

    private final CollegeRepository collegeRepository;
    private final SchoolRepository schoolRepository;

    public CollegeServiceImpl(CollegeRepository collegeRepository, SchoolRepository schoolRepository) {
        this.collegeRepository = collegeRepository;
        this.schoolRepository = schoolRepository;
    }

    @Override
    public List<College> findAll() {
        return collegeRepository.findAll();
    }

    @Override
    public Page<College> findAll(Pageable pageable) {
        return collegeRepository.findAll(pageable);
    }

    @Override
    public Optional<College> findById(Long id) {
        return collegeRepository.findById(id);
    }

    @Override
    public List<College> findBySchool(School school) {
        return collegeRepository.findBySchool(school);
    }

    @Override
    public Optional<College> findByNameAndSchool(String name, School school) {
        return collegeRepository.findByNameAndSchool(name, school);
    }

    @Override
    public College save(College college) {
        return collegeRepository.save(college);
    }

    @Override
    public void deleteById(Long id) {
        collegeRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndSchool(String name, School school) {
        return collegeRepository.existsByNameAndSchool(name, school);
    }

    @Override
    public List<College> findBySchoolId(Long schoolId) {
        return collegeRepository.findBySchoolId(schoolId);
    }

    @Override
    public Optional<College> findByNameAndSchool(String name, Long schoolId) {
        School school = schoolRepository.findById(schoolId).orElse(null);
        if (school == null) return Optional.empty();
        return collegeRepository.findByNameAndSchool(name, school);
    }

    @Override
    public boolean existsByNameAndSchool(String name, Long schoolId) {
        School school = schoolRepository.findById(schoolId).orElse(null);
        if (school == null) return false;
        return collegeRepository.existsByNameAndSchool(name, school);
    }
} 