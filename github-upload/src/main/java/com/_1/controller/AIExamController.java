package com._1.controller;

import com._1.entity.*;
import com._1.service.*;
import com._1.dto.*;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI一键出卷控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AIExamController {

    private static final Logger logger = LoggerFactory.getLogger(AIExamController.class);

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private ExamService examService;

    @Autowired
    private ClassService classService;

    @Autowired
    private QuestionService questionService;

    /**
     * AI生成试卷
     */
    @PostMapping("/generate-exam")
    public ResponseEntity<?> generateExam(@RequestBody AIExamGenerationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("接收到AI一键出卷请求: {}", request);
            
            // 验证请求参数
            Subject subject = subjectService.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("科目不存在: " + request.getSubjectId()));
            
            List<Chapter> chapters = new ArrayList<>();
            if (!request.getChapterIds().isEmpty()) {
                // 逐个查询每个章节，而不是使用findAllById
                for (Long chapterId : request.getChapterIds()) {
                    chapterService.findById(chapterId).ifPresent(chapters::add);
                }
                if (chapters.isEmpty()) {
                    throw new IllegalArgumentException("未找到指定的章节");
                }
            } else {
                chapters = chapterService.findBySubject(subject);
            }
            
            // 创建试卷模板
            Exam examTemplate = new Exam();
            examTemplate.setName(request.getName());
            // Exam类没有setDescription方法，这里使用名称或其他方式保存描述信息
            examTemplate.setDuration(request.getDuration());
            examTemplate.setSubject(subject);
            examTemplate.setStatus("PENDING"); // 草稿状态
            examTemplate.setCreator("AI助手"); // 使用setCreator代替setCreatedBy
            examTemplate.setTotalScore(request.getTargetScore());
            
            // 生成题目分布描述
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("请根据以下要求，为我生成一套 " + subject.getName() + " 考试题目：\n\n");
            promptBuilder.append("考试名称：" + request.getName() + "\n");
            promptBuilder.append("考试时长：" + request.getDuration() + "分钟\n");
            promptBuilder.append("总分值：" + request.getTargetScore() + "分\n\n");
            
            promptBuilder.append("题目类型和数量要求：\n");
            for (Map.Entry<String, QuestionPlan> entry : request.getQuestionPlan().entrySet()) {
                String questionType = entry.getKey();
                QuestionPlan plan = entry.getValue();
                if (plan.getCount() > 0) {
                    String typeDesc = "";
                    switch (questionType) {
                        case "SINGLE_CHOICE":
                            typeDesc = "单选题";
                            break;
                        case "MULTIPLE_CHOICE":
                            typeDesc = "多选题";
                            break;
                        case "FILL_BLANK":
                            typeDesc = "填空题";
                            break;
                        case "SHORT_ANSWER":
                            typeDesc = "简答题";
                            break;
                    }
                    promptBuilder.append("- " + typeDesc + "：" + plan.getCount() + "题，每题 " + plan.getScorePerQuestion() + " 分\n");
                }
            }
            
            promptBuilder.append("\n考试范围章节：\n");
            for (Chapter chapter : chapters) {
                promptBuilder.append("- " + chapter.getName() + "\n");
            }
            
            if (request.getChapterDistribution() != null && !request.getChapterDistribution().isEmpty()) {
                promptBuilder.append("\n章节分布要求：\n" + request.getChapterDistribution() + "\n");
            }
            
            if (request.getExamRequirements() != null && !request.getExamRequirements().isEmpty()) {
                promptBuilder.append("\n特殊要求：\n" + request.getExamRequirements() + "\n");
            }
            
            promptBuilder.append("\n请按以下格式生成JSON格式的考题：\n");
            promptBuilder.append("```json\n");
            promptBuilder.append("{\n");
            promptBuilder.append("  \"questions\": [\n");
            promptBuilder.append("    {\n");
            promptBuilder.append("      \"type\": \"SINGLE_CHOICE\",\n");
            promptBuilder.append("      \"content\": \"问题内容\",\n");
            promptBuilder.append("      \"score\": 分值,\n");
            promptBuilder.append("      \"options\": [\n");
            promptBuilder.append("        { \"key\": \"A\", \"content\": \"选项A\" },\n");
            promptBuilder.append("        { \"key\": \"B\", \"content\": \"选项B\" },\n");
            promptBuilder.append("        { \"key\": \"C\", \"content\": \"选项C\" },\n");
            promptBuilder.append("        { \"key\": \"D\", \"content\": \"选项D\" }\n");
            promptBuilder.append("      ],\n");
            promptBuilder.append("      \"answer\": \"正确答案（如A或[A,B]）\",\n");
            promptBuilder.append("      \"analysis\": \"答案解析\"\n");
            promptBuilder.append("    },\n");
            promptBuilder.append("    // 更多题目...\n");
            promptBuilder.append("  ]\n");
            promptBuilder.append("}\n");
            promptBuilder.append("```\n");
            promptBuilder.append("注意：生成的题目应符合大学教学质量和考试规范，难度适中，覆盖指定章节内容。");
            
            // 调用AI生成题目
            String aiPrompt = promptBuilder.toString();
            logger.info("向AI发送生成试卷提示: {}", aiPrompt);
            
            String aiResponse = deepSeekService.generateContent(aiPrompt);
            logger.info("AI生成试卷响应: {}", aiResponse);
            
            // 解析AI生成的JSON，提取问题
            List<Question> generatedQuestions = deepSeekService.parseGeneratedQuestionsFromJson(aiResponse);
            
            // 将生成的题目添加到试卷中
            examTemplate.setQuestions(generatedQuestions);
            
            // 构建返回结果
            response.put("success", true);
            response.put("exam", examTemplate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("生成试卷失败:", e);
            response.put("success", false);
            response.put("message", "生成试卷失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 保存AI生成的试卷
     */
    @PostMapping("/save-exam")
    public ResponseEntity<?> saveExam(@RequestBody SaveExamRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("接收到保存试卷请求: {}", request);
            
            // 创建考试实体
            Exam exam = new Exam();
            exam.setName(request.getName());
            // Exam类没有setDescription方法
            exam.setDuration(request.getDuration());
            exam.setTotalScore(request.getTotalScore());
            
            // 将字符串转换为Date
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date examDate = sdf.parse(request.getExamDate());
                exam.setExamDate(examDate);
            } catch (Exception ex) {
                logger.error("解析考试日期失败: {}", ex.getMessage());
                // 设置默认日期
                exam.setExamDate(new Date());
            }
            
            exam.setStatus("ACTIVE"); // 默认为活跃状态
            exam.setCreator("AI助手"); // 使用setCreator代替setCreatedBy
            
            // 设置科目
            Subject subject = subjectService.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("科目不存在: " + request.getSubjectId()));
            exam.setSubject(subject);
            
            // 设置目标班级
            if (request.getTargetClassIds() != null && !request.getTargetClassIds().isEmpty()) {
                List<ClassEntity> targetClasses = new ArrayList<>();
                for (Long classId : request.getTargetClassIds()) {
                    // 使用ClassService查找每个班级，而不是直接使用findAllById
                    classService.findById(classId).ifPresent(targetClasses::add);
                }
                exam.setTargetClasses(targetClasses);
            }
            
            // 保存题目
            List<Question> questions = new ArrayList<>();
            for (Question questionRequest : request.getQuestions()) {
                Question question = new Question();
                question.setContent(questionRequest.getContent());
                question.setType(questionRequest.getType());
                question.setScore(questionRequest.getScore());
                question.setOptions(questionRequest.getOptions());
                question.setAnswer(questionRequest.getAnswer());
                question.setAnalysis(questionRequest.getAnalysis());
                
                // Question类的setSubject接受String，而不是Subject对象
                if (subject != null) {
                    question.setSubject(subject.getName());
                } else {
                    question.setSubject("未分类");
                }
                
                // 如果有章节信息，设置章节
                if (questionRequest.getChapter() != null && questionRequest.getChapter().getId() != null) {
                    Chapter chapter = chapterService.findById(questionRequest.getChapter().getId()).orElse(null);
                    question.setChapter(chapter);
                }
                
                questions.add(question);
            }
            
            // 保存题目并关联到考试
            List<Question> savedQuestions = questionService.saveAll(questions);
            exam.setQuestions(savedQuestions);
            
            // 保存考试
            Exam savedExam = examService.save(exam);
            
            // 返回结果
            response.put("success", true);
            response.put("examId", savedExam.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("保存试卷失败:", e);
            response.put("success", false);
            response.put("message", "保存试卷失败: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * 直接导出AI生成的试卷为Word文档（不保存到数据库）
     */
    @PostMapping("/export/word")
    public ResponseEntity<byte[]> exportAIExamToWord(@RequestBody AIExamExportRequest request,
                                                     Authentication authentication) {
        try {
            logger.info("接收到AI试卷导出请求: {}", request);

            // 验证必要字段
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (request.getQuestions() == null || request.getQuestions().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 获取当前用户
            String createdBy = authentication != null ? authentication.getName() : "AI助手";

            // 生成Word文档
            byte[] wordContent = generateAIExamWordDocument(request, createdBy);

            // 设置文件名
            String fileName = request.getName() + ".docx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", encodedFileName);
            headers.add("Access-Control-Expose-Headers", "Content-Disposition");

            logger.info("AI试卷导出成功: 标题={}, 题目数量={}, 文件大小={}字节",
                       request.getName(), request.getQuestions().size(), wordContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(wordContent);

        } catch (Exception e) {
            logger.error("导出AI试卷失败: ", e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 生成AI试卷Word文档
     */
    private byte[] generateAIExamWordDocument(AIExamExportRequest request, String createdBy) throws Exception {

        // 创建Word文档
        XWPFDocument document = new XWPFDocument();

        try {
            // 设置页面边距
            document.getDocument().getBody().addNewSectPr().addNewPgMar();

            // 标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(request.getName());
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("宋体");

            // 试卷信息
            XWPFParagraph infoParagraph = document.createParagraph();
            infoParagraph.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun infoRun = infoParagraph.createRun();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
            String infoText = String.format("总分：%d分    时长：%d分钟    创建时间：%s",
                                           request.getTotalScore() != null ? request.getTotalScore() : 100,
                                           request.getDuration() != null ? request.getDuration() : 120,
                                           sdf.format(new Date()));
            infoRun.setText(infoText);
            infoRun.setFontSize(12);
            infoRun.setFontFamily("宋体");

            // 试卷描述（如果有）
            if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
                XWPFParagraph descParagraph = document.createParagraph();
                XWPFRun descRun = descParagraph.createRun();
                descRun.setText("说明：" + request.getDescription());
                descRun.setFontSize(11);
                descRun.setFontFamily("宋体");
            }

            // 分隔线
            XWPFParagraph separatorParagraph = document.createParagraph();
            XWPFRun separatorRun = separatorParagraph.createRun();
            separatorRun.setText("————————————————————————————————————————————————");
            separatorRun.setFontSize(10);

            // 按题型分组题目
            Map<String, List<Question>> questionsByType = new LinkedHashMap<>();
            for (Question question : request.getQuestions()) {
                String type = question.getType().toString();
                questionsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(question);
            }

            // 题目内容
            int questionNumber = 1;
            for (Map.Entry<String, List<Question>> entry : questionsByType.entrySet()) {
                String type = entry.getKey();
                List<Question> questions = entry.getValue();

                // 题型标题
                XWPFParagraph typeParagraph = document.createParagraph();
                XWPFRun typeRun = typeParagraph.createRun();
                String typeTitle = getQuestionTypeTitle(type, questions.size());
                typeRun.setText(typeTitle);
                typeRun.setBold(true);
                typeRun.setFontSize(14);
                typeRun.setFontFamily("宋体");

                // 该题型的题目
                for (Question question : questions) {
                    // 题目标题
                    XWPFParagraph questionParagraph = document.createParagraph();
                    XWPFRun questionRun = questionParagraph.createRun();
                    questionRun.setText(String.format("%d. %s (%d分)", questionNumber++, question.getContent(), question.getScore()));
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
            questionNumber = 1;
            for (Question question : request.getQuestions()) {
                XWPFParagraph answerParagraph = document.createParagraph();
                XWPFRun answerRun = answerParagraph.createRun();
                answerRun.setText(String.format("%d. %s", questionNumber++, question.getAnswer()));
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

    /**
     * 获取题型标题
     */
    private String getQuestionTypeTitle(String type, int count) {
        String typeDesc = "";
        switch (type) {
            case "SINGLE_CHOICE":
                typeDesc = "一、单选题";
                break;
            case "MULTIPLE_CHOICE":
                typeDesc = "二、多选题";
                break;
            case "TRUE_FALSE":
                typeDesc = "三、判断题";
                break;
            case "FILL_IN_THE_BLANK":
                typeDesc = "四、填空题";
                break;
            case "SHORT_ANSWER":
                typeDesc = "五、简答题";
                break;
            default:
                typeDesc = "题目";
        }
        return typeDesc + "（共" + count + "题）";
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health-check")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", new Date());
        response.put("message", "AI 服务正常运行中");
        return ResponseEntity.ok(response);
    }
}

/**
 * AI试卷生成请求DTO
 */
class AIExamGenerationRequest {
    private String name;
    private String description;
    private int duration;
    private int targetScore;
    private Long subjectId;
    private List<Long> chapterIds = new ArrayList<>();
    private Map<String, QuestionPlan> questionPlan = new HashMap<>();
    private String chapterDistribution;
    private String examRequirements;
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public int getTargetScore() { return targetScore; }
    public void setTargetScore(int targetScore) { this.targetScore = targetScore; }
    
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    
    public List<Long> getChapterIds() { return chapterIds; }
    public void setChapterIds(List<Long> chapterIds) { this.chapterIds = chapterIds; }
    
    public Map<String, QuestionPlan> getQuestionPlan() { return questionPlan; }
    public void setQuestionPlan(Map<String, QuestionPlan> questionPlan) { this.questionPlan = questionPlan; }
    
    public String getChapterDistribution() { return chapterDistribution; }
    public void setChapterDistribution(String chapterDistribution) { this.chapterDistribution = chapterDistribution; }
    
    public String getExamRequirements() { return examRequirements; }
    public void setExamRequirements(String examRequirements) { this.examRequirements = examRequirements; }
}

/**
 * 题目配置
 */
class QuestionPlan {
    private int count;
    private int scorePerQuestion;
    
    // Getters and Setters
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    
    public int getScorePerQuestion() { return scorePerQuestion; }
    public void setScorePerQuestion(int scorePerQuestion) { this.scorePerQuestion = scorePerQuestion; }
}

/**
 * 保存考试请求DTO
 */
class SaveExamRequest {
    private String name;
    private String description;
    private int duration;
    private int totalScore;
    private String examDate;
    private Long subjectId;
    private List<Long> targetClassIds = new ArrayList<>();
    private List<Question> questions = new ArrayList<>();
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    
    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    
    public String getExamDate() { return examDate; }
    public void setExamDate(String examDate) { this.examDate = examDate; }
    
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    
    public List<Long> getTargetClassIds() { return targetClassIds; }
    public void setTargetClassIds(List<Long> targetClassIds) { this.targetClassIds = targetClassIds; }
    
    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}

/**
 * AI试卷导出请求DTO
 */
class AIExamExportRequest {
    private String name;
    private String description;
    private Integer duration;
    private Integer totalScore;
    private List<Question> questions = new ArrayList<>();

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }
}