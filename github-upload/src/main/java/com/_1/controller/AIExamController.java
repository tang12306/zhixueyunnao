package com._1.controller;

import com._1.dto.ai.ExamExportRequest;
import com._1.dto.ai.ExamGenerationRequest;
import com._1.service.ExamWordExporter;
import com._1.service.ai.AiQuestionService;
import com._1.service.ai.AiQuestionService.ExamJob;
import com._1.service.ai.AiTaskService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * AI 一键出卷和导出 Word。失败时由 GlobalExceptionHandler 返回 4xx/5xx。
 * <p>
 * 出卷是异步的：参数校验同步完成（400/503），通过后返回 202 和任务 id，结果到 /api/ai/tasks/{id} 查询。
 */
@RestController
@RequestMapping("/api/ai")
public class AIExamController {

    private static final MediaType DOCX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final AiQuestionService aiQuestionService;
    private final ExamWordExporter examWordExporter;
    private final AiTaskService aiTaskService;

    public AIExamController(AiQuestionService aiQuestionService, ExamWordExporter examWordExporter,
                            AiTaskService aiTaskService) {
        this.aiQuestionService = aiQuestionService;
        this.examWordExporter = examWordExporter;
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/generate-exam")
    public ResponseEntity<Map<String, Object>> generateExam(@Valid @RequestBody ExamGenerationRequest request,
                                                            Authentication authentication) {
        ExamJob job = aiQuestionService.prepareExam(request);
        String taskId = aiTaskService.submit(authentication.getName(), "exam",
                stage -> aiQuestionService.runExam(job, stage));
        return AiTaskController.accepted(taskId);
    }

    /** 把还没入库的试卷直接导出为 Word */
    @PostMapping("/export/word")
    public ResponseEntity<byte[]> exportWord(@Valid @RequestBody ExamExportRequest request) throws IOException {
        byte[] content = examWordExporter.export(request);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(safeFileName(request.getName()) + ".docx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(content);
    }

    /** 只说明 AI 是否已配置，不发起外部请求 */
    @GetMapping("/health-check")
    public Map<String, Object> healthCheck() {
        boolean configured = aiQuestionService.isConfigured();
        return Map.of(
                "status", configured ? "ok" : "not_configured",
                "configured", configured,
                "message", configured ? "AI 服务已配置" : "AI 服务未配置，请设置 DEEPSEEK_API_KEY");
    }

    private static String safeFileName(String name) {
        String cleaned = name.trim().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        return cleaned.isEmpty() ? "AI试卷" : cleaned;
    }
}
