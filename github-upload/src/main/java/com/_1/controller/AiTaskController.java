package com._1.controller;

import com._1.service.ai.AiTaskService;
import com._1.service.ai.AiTaskService.TaskView;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

/**
 * 查询 AI 生成任务的进度和结果。任务失败时这里仍返回 200，失败原因在 error 字段里。
 */
@RestController
@RequestMapping("/api/ai/tasks")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @GetMapping("/{id}")
    public TaskView getTask(@PathVariable String id, Authentication authentication) {
        return aiTaskService.get(authentication.getName(), id);
    }

    /** 提交成功的响应：202 + 任务 id */
    static ResponseEntity<Map<String, Object>> accepted(String taskId) {
        return ResponseEntity.accepted()
                .location(URI.create("/api/ai/tasks/" + taskId))
                .body(Map.of("success", true, "taskId", taskId, "status", AiTaskService.Status.QUEUED));
    }
}
