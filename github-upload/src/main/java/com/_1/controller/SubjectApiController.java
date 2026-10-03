package com._1.controller;

import com._1.entity.Subject;
import com._1.service.SubjectService;
import com._1.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException; // For handling unique constraint violations

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subjects") // Changed base path to /api/subjects
@CrossOrigin(origins = {"http://localhost:8082", "http://localhost:8083", "http://localhost:8084"}) // Allowing requests from Vue frontend
public class SubjectApiController {

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private QuestionService questionService;

    // Migrated from SubjectController - GET all subjects
    @GetMapping
    public ResponseEntity<List<Subject>> getAllSubjects() {
        List<Subject> subjects = subjectService.findAll();
        return ResponseEntity.ok(subjects);
    }

    // Migrated from SubjectController - GET subject by ID
    @GetMapping("/{id}")
    public ResponseEntity<Subject> getSubjectById(@PathVariable Long id) {
        return subjectService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create new subject - NEW
    @PostMapping
    public ResponseEntity<?> createSubject(@RequestBody Subject subject) {
        if (subjectService.existsByName(subject.getName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Subject with name '" + subject.getName() + "' already exists.");
        }
        try {
            Subject savedSubject = subjectService.save(subject);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedSubject);
        } catch (DataIntegrityViolationException e) {
             // This might be redundant if existsByName is checked first, but good for other integrity issues
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating subject: " + e.getMessage());
        }
    }

    // Update existing subject - NEW
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSubject(@PathVariable Long id, @RequestBody Subject subjectDetails) {
        return subjectService.findById(id)
            .map(existingSubject -> {
                // Check if the new name conflicts with another existing subject's name
                if (subjectDetails.getName() != null && !subjectDetails.getName().equals(existingSubject.getName()) && subjectService.existsByName(subjectDetails.getName())) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("Subject with name '" + subjectDetails.getName() + "' already exists.");
                }
                
                if (subjectDetails.getName() != null) {
                    existingSubject.setName(subjectDetails.getName());
                }
                if (subjectDetails.getDescription() != null) {
                    existingSubject.setDescription(subjectDetails.getDescription());
                }
                // Note: We are not updating chapters, majors, exams lists here. 
                // That would require more complex logic or separate endpoints.

                try {
                    Subject updatedSubject = subjectService.save(existingSubject);
                    return ResponseEntity.ok(updatedSubject);
                } catch (DataIntegrityViolationException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error updating subject: " + e.getMessage());
                }
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Migrated from SubjectController - DELETE subject by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        if (!subjectService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        try {
            subjectService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Consider logging the exception e
            // Catch specific exceptions if possible, e.g., DataIntegrityViolationException if subject is in use
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null); // Or a message: "Error deleting subject. It might be in use."
        }
    }

    @GetMapping("/statistics/question-counts")
    public ResponseEntity<List<Map<String, Object>>> getSubjectQuestionCounts() {
        try {
            List<Subject> subjects = subjectService.findAll();
            List<Map<String, Object>> subjectCounts = subjects.stream().map(subject -> {
                long count = questionService.countBySubjectName(subject.getName());
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("name", subject.getName());
                map.put("value", count);
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(subjectCounts);
        } catch (Exception e) {
            // 记录错误但不在控制台输出
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ArrayList<>());
        }
    }
} 