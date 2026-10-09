package com._1.controller;

import com._1.dto.ai.BatchQuestionRequest;
import com._1.dto.ai.SaveQuestionRequest;
import com._1.service.ai.AiDraftService;
import com._1.service.ai.AiQuestionService;
import com._1.service.ai.AiQuestionService.QuestionJob;
import com._1.service.ai.AiTaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 按题型出题，以及把生成的题目存进题库。失败时由 GlobalExceptionHandler 返回 4xx/5xx。
 * <p>
 * 出题是异步的：参数校验同步完成（400/503），通过后返回 202 和任务 id，结果到 /api/ai/tasks/{id} 查询。
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AiQuestionService aiQuestionService;
    private final AiDraftService aiDraftService;
    private final AiTaskService aiTaskService;

    public AIController(AiQuestionService aiQuestionService, AiDraftService aiDraftService,
                        AiTaskService aiTaskService) {
        this.aiQuestionService = aiQuestionService;
        this.aiDraftService = aiDraftService;
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/generate-batch-questions")
    public ResponseEntity<Map<String, Object>> generateBatchQuestions(@Valid @RequestBody BatchQuestionRequest request,
                                                                      Authentication authentication) {
        QuestionJob job = aiQuestionService.prepareQuestions(request);
        String taskId = aiTaskService.submit(authentication.getName(), "questions",
                stage -> aiQuestionService.runQuestions(job, stage));
        return AiTaskController.accepted(taskId);
    }

    @PostMapping("/save-questions")
    public Map<String, Object> saveQuestions(@RequestBody List<SaveQuestionRequest> questions) {
        int saved = aiDraftService.saveQuestions(questions);
        return Map.of("success", true, "message", "成功保存 " + saved + " 道题目", "count", saved);
    }
}
