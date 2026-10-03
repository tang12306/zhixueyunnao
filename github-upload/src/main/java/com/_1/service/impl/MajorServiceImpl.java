package com._1.service.impl;

import com._1.entity.College;
import com._1.entity.Major;
import com._1.entity.Subject;
import com._1.repository.CollegeRepository;
import com._1.repository.MajorRepository;
import com._1.repository.SubjectRepository;
import com._1.service.MajorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MajorServiceImpl implements MajorService {

    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final SubjectRepository subjectRepository;

    public MajorServiceImpl(MajorRepository majorRepository, 
                            CollegeRepository collegeRepository, 
                            SubjectRepository subjectRepository) {
        this.majorRepository = majorRepository;
        this.collegeRepository = collegeRepository;
        this.subjectRepository = subjectRepository;
    }

    @Override
    public List<Major> findAll() {
        return majorRepository.findAll();
    }

    @Override
    public Page<Major> findAll(Pageable pageable) {
        return majorRepository.findAll(pageable);
    }

    @Override
    public Optional<Major> findById(Long id) {
        return majorRepository.findById(id);
    }

    @Override
    public List<Major> findByCollege(College college) {
        return majorRepository.findByCollege(college);
    }

    @Override
    public Optional<Major> findByNameAndCollege(String name, College college) {
        return majorRepository.findByNameAndCollege(name, college);
    }

    @Override
    public List<Major> findBySubject(Subject subject) {
        if (subject == null || subject.getId() == null) {
            return List.of();
        }
        return majorRepository.findBySubjectId(subject.getId());
    }

    @Override
    public Major save(Major major) {
        return majorRepository.save(major);
    }

    @Override
    public void deleteById(Long id) {
        majorRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndCollege(String name, College college) {
        return majorRepository.existsByNameAndCollege(name, college);
    }
    
    @Override
    @Transactional
    public void addSubjectToMajor(Long majorId, Long subjectId) {
        Major major = majorRepository.findById(majorId).orElseThrow(() -> new RuntimeException("Major not found"));
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new RuntimeException("Subject not found"));
        major.getSubjects().add(subject);
        majorRepository.save(major);
    }
    
    @Override
    @Transactional
    public void removeSubjectFromMajor(Long majorId, Long subjectId) {
        Major major = majorRepository.findById(majorId).orElseThrow(() -> new RuntimeException("Major not found"));
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new RuntimeException("Subject not found"));
        major.getSubjects().remove(subject);
        majorRepository.save(major);
    }

    @Override
    public List<Major> findByCollegeId(Long collegeId) {
        return majorRepository.findByCollegeId(collegeId);
    }

    @Override
    public boolean existsByNameAndCollegeId(String name, Long collegeId) {
        return majorRepository.existsByNameAndCollegeId(name, collegeId);
    }

    @Override
    public Optional<Major> findByNameAndCollegeId(String name, Long collegeId) {
        return majorRepository.findByNameAndCollegeId(name, collegeId);
    }
} 