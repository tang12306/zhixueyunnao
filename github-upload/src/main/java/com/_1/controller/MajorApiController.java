package com._1.controller;

import com._1.core.exception.ApiException;
import com._1.entity.College;
import com._1.entity.Major;
import com._1.service.CollegeService;
import com._1.service.MajorService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/majors")
public class MajorApiController {

    private final MajorService majorService;
    private final CollegeService collegeService;

    public MajorApiController(MajorService majorService, CollegeService collegeService) {
        this.majorService = majorService;
        this.collegeService = collegeService;
    }

    // 获取所有专业，可选按 collegeId 过滤
    @GetMapping
    public List<Major> getAllMajors(@RequestParam(required = false) Long collegeId) {
        return collegeId != null ? majorService.findByCollegeId(collegeId) : majorService.findAll();
    }

    @GetMapping("/{id}")
    public Major getMajorById(@PathVariable Long id) {
        return findMajor(id);
    }

    @PostMapping
    public ResponseEntity<Major> createMajor(@RequestBody Major major) {
        if (major.getCollege() == null || major.getCollege().getId() == null) {
            throw ApiException.badRequest("请选择专业所属学院");
        }
        College college = collegeService.findById(major.getCollege().getId())
                .orElseThrow(() -> ApiException.badRequest("所选学院不存在"));
        major.setCollege(college);
        if (majorService.existsByNameAndCollegeId(major.getName(), college.getId())) {
            throw ApiException.conflict("专业名称 '" + major.getName() + "' 在该学院下已存在");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(majorService.save(major));
    }

    @PutMapping("/{id}")
    public Major updateMajor(@PathVariable Long id, @RequestBody Major majorDetails) {
        Major existingMajor = findMajor(id);

        boolean collegeChanged = false;
        if (majorDetails.getCollege() != null && majorDetails.getCollege().getId() != null
                && !majorDetails.getCollege().getId().equals(existingMajor.getCollege().getId())) {
            College newCollege = collegeService.findById(majorDetails.getCollege().getId())
                    .orElseThrow(() -> ApiException.badRequest("所选学院不存在"));
            existingMajor.setCollege(newCollege);
            collegeChanged = true;
        }

        // 名称或学院变了，都要检查新组合是否重复
        if ((collegeChanged || !existingMajor.getName().equals(majorDetails.getName()))
                && majorService.existsByNameAndCollegeId(majorDetails.getName(), existingMajor.getCollege().getId())) {
            throw ApiException.conflict("专业名称 '" + majorDetails.getName() + "' 在 '"
                    + existingMajor.getCollege().getName() + "' 学院下已存在");
        }

        existingMajor.setName(majorDetails.getName());
        existingMajor.setCode(majorDetails.getCode());
        existingMajor.setDescription(majorDetails.getDescription());
        return majorService.save(existingMajor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMajor(@PathVariable Long id) {
        findMajor(id);
        try {
            majorService.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("删除专业失败，该专业下可能存在关联的班级。请先删除所有关联班级。");
        }
        return ResponseEntity.noContent().build();
    }

    private Major findMajor(Long id) {
        return majorService.findById(id).orElseThrow(() -> ApiException.notFound("专业不存在"));
    }
}
