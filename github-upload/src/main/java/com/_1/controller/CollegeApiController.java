package com._1.controller;

import com._1.core.exception.ApiException;
import com._1.entity.College;
import com._1.entity.School;
import com._1.service.CollegeService;
import com._1.service.SchoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/colleges")
public class CollegeApiController {

    private static final Logger logger = LoggerFactory.getLogger(CollegeApiController.class);
    private static final String DEFAULT_SCHOOL_NAME = "江苏师范大学";

    private final CollegeService collegeService;
    private final SchoolService schoolService;

    public CollegeApiController(CollegeService collegeService, SchoolService schoolService) {
        this.collegeService = collegeService;
        this.schoolService = schoolService;
    }

    /** 学院都挂在默认学校下；默认学校缺失属于初始化数据问题 */
    private School getDefaultSchool() {
        return schoolService.findByName(DEFAULT_SCHOOL_NAME).orElseThrow(() -> {
            logger.error("默认学校 '{}' 不存在，请检查初始化数据", DEFAULT_SCHOOL_NAME);
            return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "学校信息未初始化，请联系管理员");
        });
    }

    @GetMapping
    public List<College> getAllColleges() {
        return collegeService.findBySchoolId(getDefaultSchool().getId());
    }

    @GetMapping("/{id}")
    public College getCollegeById(@PathVariable Long id) {
        return findCollege(id);
    }

    // 创建新学院（自动关联到默认学校）
    @PostMapping
    public ResponseEntity<College> createCollege(@RequestBody College college) {
        School school = getDefaultSchool();
        college.setSchool(school);
        if (collegeService.existsByNameAndSchool(college.getName(), school.getId())) {
            throw ApiException.conflict("学院名称 '" + college.getName() + "' 已存在");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(collegeService.save(college));
    }

    @PutMapping("/{id}")
    public College updateCollege(@PathVariable Long id, @RequestBody College collegeDetails) {
        College existingCollege = findCollege(id);
        if (!existingCollege.getName().equals(collegeDetails.getName())
                && collegeService.existsByNameAndSchool(collegeDetails.getName(), existingCollege.getSchool().getId())) {
            throw ApiException.conflict("学院名称 '" + collegeDetails.getName() + "' 已存在");
        }
        existingCollege.setName(collegeDetails.getName());
        existingCollege.setDescription(collegeDetails.getDescription());
        return collegeService.save(existingCollege);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollege(@PathVariable Long id) {
        findCollege(id);
        try {
            collegeService.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("删除学院失败，该学院下可能存在关联的专业。请先删除所有关联专业。");
        }
        return ResponseEntity.noContent().build();
    }

    private College findCollege(Long id) {
        return collegeService.findById(id).orElseThrow(() -> ApiException.notFound("学院不存在"));
    }
}
