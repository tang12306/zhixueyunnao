package com._1.controller;

import com._1.entity.Chapter;
import com._1.entity.Subject;
import com._1.service.ChapterService;
import com._1.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com._1.dto.ChapterRequestPayload;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api") // Base path for API
@CrossOrigin(origins = {"http://localhost:8082", "http://localhost:8083", "http://localhost:8084"})
public class ChapterApiController {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private SubjectService subjectService;

    // GET /api/subjects/{subjectId}/chapters
    @GetMapping("/subjects/{subjectId}/chapters")
    public ResponseEntity<List<Chapter>> getChaptersBySubjectId(@PathVariable Long subjectId) {
        Subject subject = subjectService.findById(subjectId).orElse(null);
        if (subject != null) {
            List<Chapter> chapters = chapterService.findBySubject(subject);
            return ResponseEntity.ok(chapters);
        } else {
            // Subject not found, return 404 or an empty list based on preference
            // Returning an empty list might be more user-friendly for the frontend
            return ResponseEntity.ok(Collections.emptyList()); 
        }
    }
    
    @PostMapping("/chapters")
    public ResponseEntity<?> createChapter(@RequestBody ChapterRequestPayload payload) {
        if (payload.getSubjectId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Subject ID is required.");
        }
        Subject subject = subjectService.findById(payload.getSubjectId()).orElse(null);
        if (subject == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Subject not found with ID: " + payload.getSubjectId());
        }

        if (payload.getName() == null || payload.getName().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chapter name is required.");
        }

        if (chapterService.existsByNameAndSubject(payload.getName(), subject)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Chapter with name '" + payload.getName() + "' already exists in this subject.");
        }

        Chapter newChapter = new Chapter();
        newChapter.setName(payload.getName());
        newChapter.setDescription(payload.getDescription());
        newChapter.setSubject(subject);
        if (payload.getOrderNum() != null) {
            newChapter.setOrderNum(payload.getOrderNum());
        }

        try {
            Chapter savedChapter = chapterService.save(newChapter);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedChapter);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating chapter: " + e.getMessage());
        }
    }

    @PutMapping("/chapters/{id}")
    public ResponseEntity<?> updateChapter(@PathVariable Long id, @RequestBody ChapterRequestPayload payload) {
        Chapter existingChapter = chapterService.findById(id).orElse(null);
        if (existingChapter == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chapter not found with ID: " + id);
        }

        if (payload.getName() != null && !payload.getName().trim().isEmpty()) {
            // Check if name is changed and if the new name conflicts within the same subject
            if (!existingChapter.getName().equals(payload.getName()) && 
                chapterService.existsByNameAndSubject(payload.getName(), existingChapter.getSubject())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body("Chapter with name '" + payload.getName() + "' already exists in subject '" + existingChapter.getSubject().getName() + "'.");
            }
            existingChapter.setName(payload.getName());
        }

        if (payload.getDescription() != null) {
            existingChapter.setDescription(payload.getDescription());
        }
        
        if (payload.getOrderNum() != null) {
            existingChapter.setOrderNum(payload.getOrderNum());
        }
        
        // Note: Changing subjectId for an existing chapter is a more complex operation
        // and might not be desired. If payload.getSubjectId() is provided and different,
        // it implies moving the chapter. For now, we assume subjectId is not changed or ignored on update.
        // If subjectId change is required, ensure the new subject exists and handle potential name conflicts in the new subject.

        try {
            Chapter updatedChapter = chapterService.save(existingChapter); // save method handles both create and update
            return ResponseEntity.ok(updatedChapter);
        } catch (Exception e) {
            // Log the exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating chapter: " + e.getMessage());
        }
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<?> deleteChapter(@PathVariable Long id) {
        Chapter chapter = chapterService.findById(id).orElse(null);
        if (chapter == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chapter not found with ID: " + id);
        }

        // 检查是否有题目关联到此章节
        // Note: Chapter entity has a 'questions' field, but it might not be loaded by default (lazy loading)
        // A more robust way is to have a count query in QuestionService/Repository
        // For now, let's assume if chapter.getQuestions() is not empty, it means there are questions.
        // However, this requires questions to be eagerly loaded or fetched, which might not be efficient.
        
        // A better approach: Add a method to QuestionService/Repository like: countByChapter(Chapter chapter)
        // For this example, let's assume chapter.getQuestions() might work if the transaction is managed correctly or if fetched eagerly.
        // This needs to be tested. If chapter.getQuestions() is always empty due to lazy loading and no session,
        // then a dedicated service method `questionService.hasQuestionsForChapter(id)` would be necessary.

        // Assuming a method exists in ChapterService or QuestionService to check this.
        // For demonstration, let's imagine `chapterService.isChapterEmpty(id)`
        // if (!chapterService.isChapterEmpty(id)) { // You would need to implement this logic
        //     return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot delete chapter: It has associated questions.");
        // }

        // Simplistic check (might fail with lazy loading outside a transaction without proper fetching):
        // Load the chapter again with questions if necessary or use a specific query.
        // Chapter chapterWithQuestions = chapterService.findByIdWithQuestions(id); // Example method
        // if (chapterWithQuestions != null && !chapterWithQuestions.getQuestions().isEmpty()) {
        // return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Cannot delete chapter: It has associated questions.");
        // }

        // Given the current Chapter entity's JsonIgnore on questions, accessing chapter.getQuestions() here
        // might be problematic for checking. A direct query is best.
        // For now, we proceed with deletion and rely on DB constraints or future checks.
        // A more robust solution would involve a check in QuestionRepository: `boolean existsByChapterId(Long chapterId);`
        // Let's assume for now we proceed, but this is a point for future improvement for data integrity.

        try {
            chapterService.deleteById(id);
            return ResponseEntity.ok().build(); // Or ResponseEntity.noContent().build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
             // This can happen if there's a foreign key constraint from questions table to chapters table
             // and the database prevents deletion if questions are still referencing this chapter.
             return ResponseEntity.status(HttpStatus.CONFLICT)
                                  .body("Cannot delete chapter: It is referenced by existing questions. Please remove or reassign questions first.");
        } catch (Exception e) {
            // Log the exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting chapter: " + e.getMessage());
        }
    }

    // TODO: Implement GET /api/chapters/{id} (getChapterById - optional)
} 