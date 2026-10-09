package com._1.controller;

import com._1.core.exception.ApiException;
import com._1.entity.Subject;
import com._1.service.QuestionService;
import com._1.service.SubjectService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subjects")
public class SubjectApiController {

    private final SubjectService subjectService;
    private final QuestionService questionService;

    public SubjectApiController(SubjectService subjectService, QuestionService questionService) {
        this.subjectService = subjectService;
        this.questionService = questionService;
    }

    @GetMapping
    public List<Subject> getAllSubjects() {
        return subjectService.findAll();
    }

    @GetMapping("/{id}")
    public Subject getSubjectById(@PathVariable Long id) {
        return findSubject(id);
    }

    @PostMapping
    public ResponseEntity<Subject> createSubject(@RequestBody Subject subject) {
        if (subject.getName() == null || subject.getName().isBlank()) {
            throw ApiException.badRequest("科目名称不能为空");
        }
        if (subjectService.existsByName(subject.getName())) {
            throw ApiException.conflict("科目 '" + subject.getName() + "' 已存在");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.save(subject));
    }

    @PutMapping("/{id}")
    public Subject updateSubject(@PathVariable Long id, @RequestBody Subject subjectDetails) {
        Subject existingSubject = findSubject(id);
        if (subjectDetails.getName() != null && !subjectDetails.getName().equals(existingSubject.getName())
                && subjectService.existsByName(subjectDetails.getName())) {
            throw ApiException.conflict("科目 '" + subjectDetails.getName() + "' 已存在");
        }
        if (subjectDetails.getName() != null) {
            existingSubject.setName(subjectDetails.getName());
        }
        if (subjectDetails.getDescription() != null) {
            existingSubject.setDescription(subjectDetails.getDescription());
        }
        // 章节、专业、考试的关联通过各自的接口维护
        return subjectService.save(existingSubject);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        findSubject(id);
        try {
            subjectService.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("该科目下还有章节或考试，请先删除这些数据");
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistics/question-counts")
    public List<Map<String, Object>> getSubjectQuestionCounts() {
        return subjectService.findAll().stream().map(subject -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("name", subject.getName());
            map.put("value", questionService.countBySubjectName(subject.getName()));
            return map;
        }).toList();
    }

    private Subject findSubject(Long id) {
        return subjectService.findById(id).orElseThrow(() -> ApiException.notFound("科目不存在"));
    }
}
