package com._1.service.impl;

import com._1.entity.Subject;
import com._1.repository.SubjectRepository;
import com._1.service.SubjectService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;

    public SubjectServiceImpl(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    @Override
    public List<Subject> findAll() {
        return subjectRepository.findAll();
    }

    @Override
    public Optional<Subject> findById(Long id) {
        return subjectRepository.findById(id);
    }

    @Override
    public Subject save(Subject subject) {
        return subjectRepository.save(subject);
    }

    @Override
    public void deleteById(Long id) {
        subjectRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return subjectRepository.existsByName(name);
    }

    @Override
    public List<Subject> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>(); // Return empty list if input is null or empty
        }
        return subjectRepository.findAllById(ids);
    }

    @Override
    public Optional<Subject> findByName(String name) {
        return subjectRepository.findByName(name);
    }
} 