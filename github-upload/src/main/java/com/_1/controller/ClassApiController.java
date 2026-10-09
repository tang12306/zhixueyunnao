package com._1.controller;

import com._1.core.exception.ApiException;
import com._1.entity.ClassEntity;
import com._1.entity.Major;
import com._1.service.ClassService;
import com._1.service.MajorService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/classes")
public class ClassApiController {

    private final ClassService classService;
    private final MajorService majorService;

    public ClassApiController(ClassService classService, MajorService majorService) {
        this.classService = classService;
        this.majorService = majorService;
    }

    // 获取所有班级，可选按 majorId 过滤
    @GetMapping
    public List<ClassEntity> getAllClasses(@RequestParam(required = false) Long majorId) {
        return majorId != null ? classService.findByMajorId(majorId) : classService.findAll();
    }

    @GetMapping("/{id}")
    public ClassEntity getClassById(@PathVariable Long id) {
        return findClass(id);
    }

    @PostMapping
    public ResponseEntity<ClassEntity> createClass(@RequestBody ClassEntity classEntity) {
        if (classEntity.getMajor() == null || classEntity.getMajor().getId() == null) {
            throw ApiException.badRequest("请选择班级所属专业");
        }
        Major major = majorService.findById(classEntity.getMajor().getId())
                .orElseThrow(() -> ApiException.badRequest("所选专业不存在"));
        classEntity.setMajor(major);
        if (classService.existsByNameAndMajorId(classEntity.getName(), major.getId())) {
            throw ApiException.conflict("班级名称 '" + classEntity.getName() + "' 在该专业下已存在");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(classService.save(classEntity));
    }

    @PutMapping("/{id}")
    public ClassEntity updateClass(@PathVariable Long id, @RequestBody ClassEntity classDetails) {
        ClassEntity existingClass = findClass(id);

        boolean majorChanged = false;
        if (classDetails.getMajor() != null && classDetails.getMajor().getId() != null
                && !classDetails.getMajor().getId().equals(existingClass.getMajor().getId())) {
            Major newMajor = majorService.findById(classDetails.getMajor().getId())
                    .orElseThrow(() -> ApiException.badRequest("所选专业不存在"));
            existingClass.setMajor(newMajor);
            majorChanged = true;
        }

        boolean nameChanged = !existingClass.getName().equals(classDetails.getName());
        if ((nameChanged || majorChanged)
                && classService.existsByNameAndMajorId(classDetails.getName(), existingClass.getMajor().getId())) {
            throw ApiException.conflict("班级名称 '" + classDetails.getName() + "' 在 '"
                    + existingClass.getMajor().getName() + "' 专业下已存在");
        }

        existingClass.setName(classDetails.getName());
        existingClass.setGrade(classDetails.getGrade());
        return classService.save(existingClass);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        ClassEntity classEntity = findClass(id);

        long studentCount = classService.countStudentsInClass(id);
        if (studentCount > 0) {
            throw new ApiException(HttpStatus.CONFLICT,
                    String.format("无法删除班级 \"%s\"，该班级下还有 %d 名学生。请先将学生转移到其他班级或删除学生后再删除班级。",
                            classEntity.getName(), studentCount),
                    Map.of("studentCount", studentCount, "className", classEntity.getName()), null);
        }
        try {
            classService.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("删除班级失败，该班级下可能存在关联的考试或其他数据。请先处理关联数据。");
        }
        return ResponseEntity.noContent().build();
    }

    // 检查班级是否可以删除（用于前端预检查）
    @GetMapping("/{id}/can-delete")
    public Map<String, Object> checkCanDeleteClass(@PathVariable Long id) {
        ClassEntity classEntity = findClass(id);
        long studentCount = classService.countStudentsInClass(id);
        boolean canDelete = studentCount == 0;
        return Map.of(
                "canDelete", canDelete,
                "studentCount", studentCount,
                "className", classEntity.getName(),
                "message", canDelete ? "可以删除该班级" : String.format("该班级下还有 %d 名学生，请先转移学生", studentCount));
    }

    private ClassEntity findClass(Long id) {
        return classService.findById(id).orElseThrow(() -> ApiException.notFound("班级不存在"));
    }
}
