package com._1.controller;

import com._1.entity.College;
import com._1.entity.Major;
import com._1.service.CollegeService;
import com._1.service.MajorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/majors")
public class MajorApiController {

    private final MajorService majorService;
    private final CollegeService collegeService; // 用于验证学院是否存在

    @Autowired
    public MajorApiController(MajorService majorService, CollegeService collegeService) {
        this.majorService = majorService;
        this.collegeService = collegeService;
    }

    // 获取所有专业，可选按 collegeId 过滤
    @GetMapping
    public ResponseEntity<List<Major>> getAllMajors(@RequestParam(required = false) Long collegeId) {
        List<Major> majors;
        if (collegeId != null) {
            majors = majorService.findByCollegeId(collegeId);
        } else {
            majors = majorService.findAll();
        }
        return ResponseEntity.ok(majors);
    }

    // 根据ID获取专业
    @GetMapping("/{id}")
    public ResponseEntity<Major> getMajorById(@PathVariable Long id) {
        return majorService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 创建新专业
    @PostMapping
    public ResponseEntity<?> createMajor(@RequestBody Major major) {
        if (major.getCollege() == null || major.getCollege().getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "创建专业时必须关联一个学院ID。"));
        }
        Optional<College> collegeOptional = collegeService.findById(major.getCollege().getId());
        if (collegeOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "关联的学院ID不存在。"));
        }
        major.setCollege(collegeOptional.get()); // 确保关联的是完整的College对象

        try {
            // 检查同名专业是否已存在于该学院
            if (majorService.existsByNameAndCollegeId(major.getName(), major.getCollege().getId())) {
                 return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "专业名称 '" + major.getName() + "' 在该学院下已存在。"));
            }
            Major savedMajor = majorService.save(major);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMajor);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "创建专业失败，数据校验错误: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "创建专业时发生未知错误: " + e.getMessage()));
        }
    }

    // 更新专业
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMajor(@PathVariable Long id, @RequestBody Major majorDetails) {
        Optional<Major> existingMajorOptional = majorService.findById(id);
        if (existingMajorOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Major existingMajor = existingMajorOptional.get();

        // 更新学院关联 (如果提供了新的collegeId且有效)
        if (majorDetails.getCollege() != null && majorDetails.getCollege().getId() != null) {
            if (!majorDetails.getCollege().getId().equals(existingMajor.getCollege().getId())) {
                Optional<College> newCollegeOptional = collegeService.findById(majorDetails.getCollege().getId());
                if (newCollegeOptional.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "要关联的新学院ID不存在。"));
                }
                existingMajor.setCollege(newCollegeOptional.get());
            }
        }

        // 检查名称是否更改，如果更改，新名称是否与所关联学院下其他专业冲突
        if (!existingMajor.getName().equals(majorDetails.getName()) || 
            (majorDetails.getCollege() != null && !majorDetails.getCollege().getId().equals(existingMajor.getCollege().getId())) ){
             // 如果名称或学院ID变了，都需要检查新组合的唯一性
            if (majorService.existsByNameAndCollegeId(majorDetails.getName(), existingMajor.getCollege().getId())) {
                 return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "专业名称 '" + majorDetails.getName() + "' 在 '" + existingMajor.getCollege().getName() + "' 学院下已存在。"));
            }
        }

        existingMajor.setName(majorDetails.getName());
        existingMajor.setCode(majorDetails.getCode());
        existingMajor.setDescription(majorDetails.getDescription());

        try {
            Major updatedMajor = majorService.save(existingMajor);
            return ResponseEntity.ok(updatedMajor);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "更新专业失败，数据校验错误: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "更新专业时发生未知错误: " + e.getMessage()));
        }
    }

    // 删除专业
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMajor(@PathVariable Long id) {
        if (majorService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // 注意：需要考虑级联删除或是否有班级关联到此专业
        try {
            majorService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
             return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "删除专业失败，该专业下可能存在关联的班级。请先删除所有关联班级。"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "删除专业时发生未知错误: " + e.getMessage()));
        }
    }
} 