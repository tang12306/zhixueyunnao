package com._1.controller;

import com._1.service.DeepSeekService;
import com._1.service.SubjectService;
import com._1.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com._1.entity.Subject;
import com._1.entity.Chapter;

@Controller
public class AIQuestionController {
    
    private static final Logger logger = LoggerFactory.getLogger(AIQuestionController.class);
    private final DeepSeekService deepSeekService;
    private final SubjectService subjectService;
    private final ChapterService chapterService;
    
    @Autowired
    public AIQuestionController(DeepSeekService deepSeekService, 
                                SubjectService subjectService, 
                                ChapterService chapterService) {
        this.deepSeekService = deepSeekService;
        this.subjectService = subjectService;
        this.chapterService = chapterService;
    }
    
    @PostMapping("/api/generate-question")
    @ResponseBody
    public ResponseEntity<?> generateQuestion(
            @RequestParam Long subjectId,
            @RequestParam Long chapterId,
            @RequestParam String type,
            @RequestParam(required = false) String difficulty) {
        try {
            Subject subject = subjectService.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + subjectId + " 的学科"));
            Chapter chapter = chapterService.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + chapterId + " 的章节"));

            logger.info("收到生成题目请求 - 科目ID: {}, 章节ID: {}, 类型: {}, 难度: {}", 
                    subjectId, chapterId, type, difficulty);
            
            String chapterDescription = chapter.getDescription() != null ? chapter.getDescription() : "无详细描述";

            String prompt = String.format("""
                请为学科《%s》生成一道关于章节《%s》（章节主要内容：%s）的%s题目。
                题目难度请设定为%s。
                要求：
                1. 题目内容必须与章节内容紧密相关，并符合教学大纲要求。
                2. 如果是选择题，请提供4个选项，并明确标出正确答案（例如：答案：A）。
                3. 如果是填空题，请提供准确的答案。
                4. 如果是判断题，请提供正确答案（正确/错误）。
                5. 如果是简答题，请提供清晰的参考答案和评分要点。
                请严格按照以下格式输出，确保各项内容完整：
                题目：[题目内容]
                选项：（如果是选择题，每行一个选项，例如 A. xxx B. xxx C. xxx D. xxx）
                答案：[答案内容]
                解析：[对题目和答案的详细解析，说明为什么这样解答]
                """, 
                subject.getName(), chapter.getName(), chapterDescription, type, 
                difficulty != null ? difficulty : "适中");
                
            logger.info("生成的提示词: {}", prompt);
            
            String generatedQuestion = deepSeekService.generateQuestion(prompt);
            logger.info("成功生成题目: {}", generatedQuestion);
            
            return ResponseEntity.ok(generatedQuestion);
        } catch (Exception e) {
            logger.error("生成题目失败", e);
            return ResponseEntity.badRequest().body("生成题目失败：" + e.getMessage());
        }
    }
} 