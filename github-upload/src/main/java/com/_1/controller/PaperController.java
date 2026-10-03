package com._1.controller;

import com._1.entity.Question;
import com._1.service.QuestionService;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 试卷管理控制器
 */
@RestController
@RequestMapping("/api/papers")
public class PaperController {

    private static final Logger logger = LoggerFactory.getLogger(PaperController.class);

    @Autowired
    private QuestionService questionService;

    /**
     * 直接导出试卷为Word文档（不保存到数据库）
     */
    @PostMapping("/export/word")
    public ResponseEntity<byte[]> exportPaperToWord(@RequestBody Map<String, Object> paperData,
                                                    Authentication authentication) {
        try {
            logger.info("接收到试卷导出请求: {}", paperData);

            // 提取试卷基本信息
            String title = (String) paperData.get("title");
            String description = (String) paperData.get("description");
            Object durationObj = paperData.get("duration");
            String subject = (String) paperData.get("subject");

            // 获取当前用户
            String createdBy = authentication != null ? authentication.getName() : "system";

            // 验证必要字段
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 处理题目列表
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionsList = (List<Map<String, Object>>) paperData.get("questions");

            if (questionsList == null || questionsList.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 验证题目ID并获取题目详情
            List<Map<String, Object>> questionsWithDetails = new ArrayList<>();
            int totalScore = 0;

            for (int i = 0; i < questionsList.size(); i++) {
                Map<String, Object> questionItem = questionsList.get(i);
                Object questionIdObj = questionItem.get("questionId");
                Object scoreObj = questionItem.get("score");

                if (questionIdObj == null || scoreObj == null) {
                    return ResponseEntity.badRequest().build();
                }

                Long questionId = Long.valueOf(questionIdObj.toString());
                Integer score = Integer.valueOf(scoreObj.toString());

                // 获取题目详情
                Optional<Question> questionOpt = questionService.findById(questionId);
                if (!questionOpt.isPresent()) {
                    return ResponseEntity.badRequest().build();
                }

                Question question = questionOpt.get();
                Map<String, Object> questionWithDetails = new HashMap<>();
                questionWithDetails.put("question", question);
                questionWithDetails.put("score", score);
                questionWithDetails.put("orderNum", i + 1);

                questionsWithDetails.add(questionWithDetails);
                totalScore += score;
            }

            // 生成Word文档
            Integer duration = durationObj != null ? Integer.valueOf(durationObj.toString()) : 120;
            byte[] wordContent = generateWordDocument(title, description, subject, duration,
                                                    createdBy, totalScore, questionsWithDetails);

            // 设置文件名
            String fileName = title + ".docx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            logger.info("试卷导出成功: 标题={}, 题目数量={}, 总分={}, 文件大小={}字节",
                       title, questionsList.size(), totalScore, wordContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(wordContent);

        } catch (Exception e) {
            logger.error("导出试卷失败: ", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 生成Word文档
     */
    private byte[] generateWordDocument(String title, String description, String subject,
                                      Integer duration, String createdBy, int totalScore,
                                      List<Map<String, Object>> questionsWithDetails) throws Exception {

        // 创建Word文档
        XWPFDocument document = new XWPFDocument();

        try {
            // 设置页面边距
            document.getDocument().getBody().addNewSectPr().addNewPgMar();

            // 标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("宋体");

            // 试卷信息
            XWPFParagraph infoParagraph = document.createParagraph();
            infoParagraph.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun infoRun = infoParagraph.createRun();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
            String infoText = String.format("科目：%s    总分：%d分    时长：%d分钟    创建时间：%s",
                                           subject != null ? subject : "综合",
                                           totalScore,
                                           duration,
                                           sdf.format(new Date()));
            infoRun.setText(infoText);
            infoRun.setFontSize(12);
            infoRun.setFontFamily("宋体");

            // 试卷描述（如果有）
            if (description != null && !description.trim().isEmpty()) {
                XWPFParagraph descParagraph = document.createParagraph();
                XWPFRun descRun = descParagraph.createRun();
                descRun.setText("说明：" + description);
                descRun.setFontSize(11);
                descRun.setFontFamily("宋体");
            }

            // 分隔线
            XWPFParagraph separatorParagraph = document.createParagraph();
            XWPFRun separatorRun = separatorParagraph.createRun();
            separatorRun.setText("————————————————————————————————————————————————");
            separatorRun.setFontSize(10);

            // 题目内容
            for (Map<String, Object> questionWithDetails : questionsWithDetails) {
                Question question = (Question) questionWithDetails.get("question");
                Integer score = (Integer) questionWithDetails.get("score");
                Integer orderNum = (Integer) questionWithDetails.get("orderNum");

                // 题目标题
                XWPFParagraph questionParagraph = document.createParagraph();
                XWPFRun questionRun = questionParagraph.createRun();
                questionRun.setText(String.format("%d. %s (%d分)", orderNum, question.getContent(), score));
                questionRun.setBold(true);
                questionRun.setFontSize(12);
                questionRun.setFontFamily("宋体");

                // 选项（如果有）
                if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                    for (int j = 0; j < question.getOptions().size(); j++) {
                        XWPFParagraph optionParagraph = document.createParagraph();
                        XWPFRun optionRun = optionParagraph.createRun();
                        optionRun.setText(String.format("   %s. %s",
                                        (char)('A' + j), question.getOptions().get(j)));
                        optionRun.setFontSize(11);
                        optionRun.setFontFamily("宋体");
                    }
                }

                // 空行
                document.createParagraph();
            }

            // 答案部分
            XWPFParagraph answerTitleParagraph = document.createParagraph();
            answerTitleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun answerTitleRun = answerTitleParagraph.createRun();
            answerTitleRun.setText("参考答案");
            answerTitleRun.setBold(true);
            answerTitleRun.setFontSize(14);
            answerTitleRun.setFontFamily("宋体");

            // 答案内容
            for (Map<String, Object> questionWithDetails : questionsWithDetails) {
                Question question = (Question) questionWithDetails.get("question");
                Integer orderNum = (Integer) questionWithDetails.get("orderNum");

                XWPFParagraph answerParagraph = document.createParagraph();
                XWPFRun answerRun = answerParagraph.createRun();
                answerRun.setText(String.format("%d. %s", orderNum, question.getAnswer()));
                answerRun.setFontSize(11);
                answerRun.setFontFamily("宋体");

                // 解析（如果有）
                if (question.getAnalysis() != null && !question.getAnalysis().trim().isEmpty()) {
                    XWPFParagraph analysisParagraph = document.createParagraph();
                    XWPFRun analysisRun = analysisParagraph.createRun();
                    analysisRun.setText("   解析：" + question.getAnalysis());
                    analysisRun.setFontSize(10);
                    analysisRun.setFontFamily("宋体");
                    analysisRun.setColor("666666");
                }
            }

            // 转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();

        } finally {
            document.close();
        }
    }
}
