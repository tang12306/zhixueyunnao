package com._1.controller;

import com._1.core.exception.ApiException;
import com._1.dto.ChapterRequestPayload;
import com._1.entity.Chapter;
import com._1.entity.Subject;
import com._1.service.ChapterService;
import com._1.service.SubjectService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ChapterApiController {

    private final ChapterService chapterService;
    private final SubjectService subjectService;

    public ChapterApiController(ChapterService chapterService, SubjectService subjectService) {
        this.chapterService = chapterService;
        this.subjectService = subjectService;
    }

    // GET /api/subjects/{subjectId}/chapters；学科不存在时返回空列表，前端下拉框不用特殊处理
    @GetMapping("/subjects/{subjectId}/chapters")
    public List<Chapter> getChaptersBySubjectId(@PathVariable Long subjectId) {
        return subjectService.findById(subjectId)
                .map(chapterService::findBySubject)
                .orElse(Collections.emptyList());
    }

    @PostMapping("/chapters")
    public ResponseEntity<Chapter> createChapter(@RequestBody ChapterRequestPayload payload) {
        if (payload.getSubjectId() == null) {
            throw ApiException.badRequest("请选择所属科目");
        }
        Subject subject = subjectService.findById(payload.getSubjectId())
                .orElseThrow(() -> ApiException.notFound("科目不存在"));
        if (payload.getName() == null || payload.getName().trim().isEmpty()) {
            throw ApiException.badRequest("章节名称不能为空");
        }
        if (chapterService.existsByNameAndSubject(payload.getName(), subject)) {
            throw ApiException.conflict("该科目下已有同名章节");
        }

        Chapter newChapter = new Chapter();
        newChapter.setName(payload.getName());
        newChapter.setDescription(payload.getDescription());
        newChapter.setSubject(subject);
        if (payload.getOrderNum() != null) {
            newChapter.setOrderNum(payload.getOrderNum());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(chapterService.save(newChapter));
    }

    @PutMapping("/chapters/{id}")
    public Chapter updateChapter(@PathVariable Long id, @RequestBody ChapterRequestPayload payload) {
        Chapter existingChapter = chapterService.findById(id)
                .orElseThrow(() -> ApiException.notFound("章节不存在"));

        if (payload.getName() != null && !payload.getName().trim().isEmpty()) {
            if (!existingChapter.getName().equals(payload.getName())
                    && chapterService.existsByNameAndSubject(payload.getName(), existingChapter.getSubject())) {
                throw ApiException.conflict("该科目下已有同名章节");
            }
            existingChapter.setName(payload.getName());
        }
        if (payload.getDescription() != null) {
            existingChapter.setDescription(payload.getDescription());
        }
        if (payload.getOrderNum() != null) {
            existingChapter.setOrderNum(payload.getOrderNum());
        }
        // 不支持通过更新把章节移到其他科目
        return chapterService.save(existingChapter);
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Long id) {
        if (chapterService.findById(id).isEmpty()) {
            throw ApiException.notFound("章节不存在");
        }
        try {
            chapterService.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict("该章节下还有题目，请先删除或移动这些题目");
        }
        return ResponseEntity.noContent().build();
    }
}
