package com._1.controller;

import com._1.entity.College;
import com._1.entity.School;
import com._1.service.CollegeService;
import com._1.service.SchoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/colleges")
public class CollegeApiController {

    private final CollegeService collegeService;
    private final SchoolService schoolService;
    private static final String DEFAULT_SCHOOL_NAME = "江苏师范大学";

    @Autowired
    public CollegeApiController(CollegeService collegeService, SchoolService schoolService) {
        this.collegeService = collegeService;
        this.schoolService = schoolService;
    }

    private Optional<School> getDefaultSchool() {
        return schoolService.findByName(DEFAULT_SCHOOL_NAME);
    }

    // 获取所有学院
    @GetMapping
    public ResponseEntity<List<College>> getAllColleges() {
        Optional<School> defaultSchool = getDefaultSchool();
        if (defaultSchool.isPresent()) {
            List<College> colleges = collegeService.findBySchoolId(defaultSchool.get().getId());
            return ResponseEntity.ok(colleges);
        } else {
            // 如果默认学校不存在，这算是一个系统级错误，或者说配置问题
            // 返回空列表并记录错误，或者返回一个特定的错误响应
            // log.error("Default school '{}' not found.", DEFAULT_SCHOOL_NAME);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of()); // 返回空列表
        }
    }

    // 根据ID获取学院
    @GetMapping("/{id}")
    public ResponseEntity<College> getCollegeById(@PathVariable Long id) {
        return collegeService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 创建新学院 (自动关联到 "江苏师范大学")
    @PostMapping
    public ResponseEntity<?> createCollege(@RequestBody College college) {
        Optional<School> defaultSchoolOptional = getDefaultSchool();
        if (defaultSchoolOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "默认学校 '" + DEFAULT_SCHOOL_NAME + "' 未找到，无法创建学院。"));
        }
        college.setSchool(defaultSchoolOptional.get());
        try {
            // 检查同名学院是否已存在于该学校
            if (collegeService.existsByNameAndSchool(college.getName(), defaultSchoolOptional.get().getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "学院名称 '" + college.getName() + "' 在该学校下已存在。"));
            }
            College savedCollege = collegeService.save(college);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCollege);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "创建学院失败，数据校验错误: " + e.getMessage()));
        } catch (Exception e) {
            // log.error("Error creating college: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "创建学院时发生未知错误: " + e.getMessage()));
        }
    }

    // 更新学院
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollege(@PathVariable Long id, @RequestBody College collegeDetails) {
        Optional<College> existingCollegeOptional = collegeService.findById(id);
        if (existingCollegeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        College existingCollege = existingCollegeOptional.get();
        
        // 检查名称是否更改，如果更改，新名称是否与该学校下其他学院冲突
        if (!existingCollege.getName().equals(collegeDetails.getName())) {
            if (collegeService.existsByNameAndSchool(collegeDetails.getName(), existingCollege.getSchool().getId())) {
                 return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "学院名称 '" + collegeDetails.getName() + "' 在该学校下已存在。"));
            }
        }

        existingCollege.setName(collegeDetails.getName());
        existingCollege.setDescription(collegeDetails.getDescription());

        try {
            College updatedCollege = collegeService.save(existingCollege);
            return ResponseEntity.ok(updatedCollege);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "更新学院失败，数据校验错误: " + e.getMessage()));
        } catch (Exception e) {
            // log.error("Error updating college: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "更新学院时发生未知错误: " + e.getMessage()));
        }
    }

    // 删除学院
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCollege(@PathVariable Long id) {
        Optional<College> collegeOptional = collegeService.findById(id);
        if (collegeOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // 检查是否有专业关联到此学院，如果有关联则不允许删除
        // This requires MajorService to have a method like countByCollegeId(Long collegeId) or findByCollegeId
        // For now, we'll rely on DataIntegrityViolationException if majors are linked with foreign key constraint
        try {
            collegeService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
             return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "删除学院失败，该学院下可能存在关联的专业。请先删除所有关联专业。"));
        } catch (Exception e) {
            // log.error("Error deleting college: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "删除学院时发生未知错误: " + e.getMessage()));
        }
    }
} 