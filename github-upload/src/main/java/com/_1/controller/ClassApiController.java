package com._1.controller;

import com._1.entity.ClassEntity;
import com._1.entity.Major;
import com._1.service.ClassService; // Assuming ClassService exists
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
@RequestMapping("/api/classes")
public class ClassApiController {

    private final ClassService classService;
    private final MajorService majorService; // To validate major exists

    @Autowired
    public ClassApiController(ClassService classService, MajorService majorService) {
        this.classService = classService;
        this.majorService = majorService;
    }

    // 获取所有班级，可选按 majorId 过滤
    @GetMapping
    public ResponseEntity<List<ClassEntity>> getAllClasses(@RequestParam(required = false) Long majorId) {
        List<ClassEntity> classes;
        if (majorId != null) {
            classes = classService.findByMajorId(majorId);
        } else {
            classes = classService.findAll();
        }
        return ResponseEntity.ok(classes);
    }

    // 根据ID获取班级
    @GetMapping("/{id}")
    public ResponseEntity<ClassEntity> getClassById(@PathVariable Long id) {
        return classService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 创建新班级
    @PostMapping
    public ResponseEntity<?> createClass(@RequestBody ClassEntity classEntity) {
        if (classEntity.getMajor() == null || classEntity.getMajor().getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "创建班级时必须关联一个专业ID。"));
        }
        Optional<Major> majorOptional = majorService.findById(classEntity.getMajor().getId());
        if (majorOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "关联的专业ID不存在。"));
        }
        classEntity.setMajor(majorOptional.get()); // Ensure full Major object is set

        try {
            // Check for duplicate class name within the same major
            if (classService.existsByNameAndMajorId(classEntity.getName(), classEntity.getMajor().getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "班级名称 '" + classEntity.getName() + "' 在该专业下已存在。"));
            }
            ClassEntity savedClass = classService.save(classEntity);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedClass);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "创建班级失败，数据校验错误: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "创建班级时发生未知错误: " + e.getMessage()));
        }
    }

    // 更新班级
    @PutMapping("/{id}")
    public ResponseEntity<?> updateClass(@PathVariable Long id, @RequestBody ClassEntity classDetails) {
        Optional<ClassEntity> existingClassOptional = classService.findById(id);
        if (existingClassOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClassEntity existingClass = existingClassOptional.get();

        // Update major if provided and valid
        if (classDetails.getMajor() != null && classDetails.getMajor().getId() != null) {
            if (!classDetails.getMajor().getId().equals(existingClass.getMajor().getId())) {
                Optional<Major> newMajorOptional = majorService.findById(classDetails.getMajor().getId());
                if (newMajorOptional.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "要关联的新专业ID不存在。"));
                }
                existingClass.setMajor(newMajorOptional.get());
            }
        }
        
        // Check for duplicate name if name or major has changed
        boolean nameChanged = !existingClass.getName().equals(classDetails.getName());
        boolean majorChanged = classDetails.getMajor() != null && !classDetails.getMajor().getId().equals(existingClass.getMajor().getId());

        if (nameChanged || majorChanged) {
            if (classService.existsByNameAndMajorId(classDetails.getName(), existingClass.getMajor().getId())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("success", false, "message", "班级名称 '" + classDetails.getName() + "' 在 '" + existingClass.getMajor().getName() + "' 专业下已存在。"));
            }
        }

        existingClass.setName(classDetails.getName());
        existingClass.setGrade(classDetails.getGrade());
        // studentCount might be managed differently (e.g., calculated)

        try {
            ClassEntity updatedClass = classService.save(existingClass);
            return ResponseEntity.ok(updatedClass);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "更新班级失败，数据校验错误: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "更新班级时发生未知错误: " + e.getMessage()));
        }
    }

    // 删除班级
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable Long id) {
        Optional<ClassEntity> classOptional = classService.findById(id);
        if (classOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClassEntity classEntity = classOptional.get();

        // 检查班级下是否有学生
        long studentCount = classService.countStudentsInClass(id);
        if (studentCount > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                        "success", false,
                        "message", String.format("无法删除班级 \"%s\"，该班级下还有 %d 名学生。请先将学生转移到其他班级或删除学生后再删除班级。",
                                classEntity.getName(), studentCount),
                        "studentCount", studentCount,
                        "className", classEntity.getName()
                    ));
        }

        // 如果没有学生，执行删除
        try {
            classService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "message", "删除班级失败，该班级下可能存在关联的考试或其他数据。请先处理关联数据。"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "删除班级时发生未知错误: " + e.getMessage()));
        }
    }

    // 检查班级是否可以删除（用于前端预检查）
    @GetMapping("/{id}/can-delete")
    public ResponseEntity<?> checkCanDeleteClass(@PathVariable Long id) {
        Optional<ClassEntity> classOptional = classService.findById(id);
        if (classOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ClassEntity classEntity = classOptional.get();
        long studentCount = classService.countStudentsInClass(id);
        boolean canDelete = studentCount == 0;

        return ResponseEntity.ok(Map.of(
            "canDelete", canDelete,
            "studentCount", studentCount,
            "className", classEntity.getName(),
            "message", canDelete ? "可以删除该班级" :
                String.format("该班级下还有 %d 名学生，请先转移学生", studentCount)
        ));
    }
}