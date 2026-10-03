package com._1.service.impl;

import com._1.entity.School;
import com._1.repository.SchoolRepository;
import com._1.service.SchoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SchoolServiceImpl implements SchoolService {

    @Autowired
    private SchoolRepository schoolRepository;

    @Override
    public List<School> findAll() {
        return schoolRepository.findAll();
    }

    @Override
    public Page<School> findAll(Pageable pageable) {
        return schoolRepository.findAll(pageable);
    }

    @Override
    public Optional<School> findById(Long id) {
        return schoolRepository.findById(id);
    }

    @Override
    public Optional<School> findByName(String name) {
        return schoolRepository.findByName(name);
    }

    @Override
    public School save(School school) {
        return schoolRepository.save(school);
    }

    @Override
    public void deleteById(Long id) {
        schoolRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return schoolRepository.existsByName(name);
    }
} 