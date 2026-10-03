/*
package com._1.service.impl;

import com._1.entity.ClassEntity;
import com._1.entity.Student;
import com._1.repository.StudentRepository;
import com._1.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Override
    @Transactional
    public Student saveStudent(Student student) {
        // Add any business logic before saving, e.g., validation
        return studentRepository.save(student);
    }

    @Override
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    @Override
    public Optional<Student> findByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    @Override
    public List<Student> findAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Page<Student> findAllStudents(Pageable pageable) {
        return studentRepository.findAll(pageable);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        studentRepository.deleteById(id);
    }

    @Override
    public List<Student> findByClass(ClassEntity classEntity) {
        return studentRepository.findByStudentClass_Id(classEntity.getId()); // Assuming repository method findByStudentClass_Id
    }

    @Override
    public Page<Student> findByClass(ClassEntity classEntity, Pageable pageable) {
        // This needs a corresponding method in StudentRepository that accepts Pageable
        // For example: studentRepository.findByStudentClass(classEntity, pageable);
        // return studentRepository.findByStudentClass(classEntity, pageable); // Placeholder
        throw new UnsupportedOperationException("Pageable findByClass not yet fully implemented in repository or this example.");
    }

    @Override
    public Page<Student> searchStudents(String keyword, Pageable pageable) {
        // This needs a corresponding method in StudentRepository
        // For example: studentRepository.findByNameContainingIgnoreCaseOrStudentIdContainingIgnoreCase(keyword, keyword, pageable);
        // return studentRepository.searchStudents(keyword, pageable); // Placeholder
        throw new UnsupportedOperationException("Search students not yet fully implemented in repository or this example.");
    }
    
    @Override
    @Transactional
    public Student updateStudent(Student student) {
        if (student.getId() == null || !studentRepository.existsById(student.getId())) {
            throw new IllegalArgumentException("Student with id " + student.getId() + " not found, cannot update.");
        }
        // Add any update-specific business logic here
        return studentRepository.save(student);
    }
}
*/ 