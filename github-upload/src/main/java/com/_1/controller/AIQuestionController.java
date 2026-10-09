package com._1.controller;

import com._1.dto.ai.BatchQuestionRequest;
import com._1.service.ai.AiQuestionService;
import com._1.service.ai.QuestionDraft;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 旧版题目编辑页（question_form.html）的“AI 生成题目”按钮：按学科、章节和题型生成一道题。
 */
@RestController
public class AIQuestionController {

    private final AiQuestionService aiQuestionService;

    public AIQuestionController(AiQuestionService aiQuestionService) {
        this.aiQuestionService = aiQuestionService;
    }

    @PostMapping("/api/generate-question")
    public QuestionDraft generateQuestion(@RequestParam Long subjectId,
                                          @RequestParam(required = false) Long chapterId,
                                          @RequestParam String type,
                                          @RequestParam(required = false) Integer difficulty) {
        BatchQuestionRequest request = new BatchQuestionRequest();
        request.setSubjectId(subjectId);
        request.setChapterIds(chapterId == null ? null : List.of(chapterId));
        request.setType(type);
        request.setDifficulty(difficulty == null || difficulty < 1 || difficulty > 5 ? null : difficulty);
        request.setCount(1);
        return aiQuestionService.generateQuestions(request).get(0);
    }
}
