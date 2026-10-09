package com._1.controller;

import com._1.entity.Chapter;
import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.service.ChapterService;
import com._1.service.ClassService;
import com._1.service.ExamService;
import com._1.service.QuestionService;
import com._1.service.SubjectService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/exam-generator")
public class ExamGeneratorController {

    private final SubjectService subjectService;
    private final ChapterService chapterService;
    private final QuestionService questionService;
    private final ExamService examService;
    private final ClassService classService;

    public ExamGeneratorController(SubjectService subjectService,
                                   ChapterService chapterService,
                                   QuestionService questionService,
                                   ExamService examService,
                                   ClassService classService) {
        this.subjectService = subjectService;
        this.chapterService = chapterService;
        this.questionService = questionService;
        this.examService = examService;
        this.classService = classService;
    }
    
    @GetMapping
    public String showGenerator(Model model) {
        List<Subject> subjects = subjectService.findAll();
        List<ClassEntity> classes = classService.findAll();
        model.addAttribute("subjects", subjects);
        model.addAttribute("classes", classes);
        model.addAttribute("questionTypes", new String[]{"CHOICE", "FILL_BLANK", "TRUE_FALSE", "SHORT_ANSWER"});
        return "exam_generator";
    }
    
    @GetMapping("/chapters")
    @ResponseBody
    public List<Chapter> getChaptersBySubject(@RequestParam Long subjectId) {
        Subject subject = subjectService.findById(subjectId).orElse(null);
        if (subject != null) {
            return chapterService.findBySubject(subject);
        }
        return List.of();
    }
    
    @PostMapping("/generate")
    public String generateExam(
            @RequestParam String examName,
            @RequestParam String examType,
            @RequestParam Long subjectId,
            @RequestParam(required = false) List<Long> chapterIds,
            @RequestParam Map<String, String> counts,
            @RequestParam(required = false) List<Long> classIds,
            Model model) {
        
        Subject subject = subjectService.findById(subjectId).orElse(null);
        if (subject == null) {
            return "redirect:/exam-generator?error=subject";
        }
        
        List<Question> generatedQuestions = new ArrayList<>();
        
        // 根据考试类型生成题目
        switch (examType) {
            case "chapter":
                // 章节测试：只从选定的章节中出题
                if (chapterIds != null && !chapterIds.isEmpty()) {
                    generateQuestionsByType(chapterIds, counts, generatedQuestions);
                }
                break;
                
            case "midterm":
                // 期中测试：从学科的前一半章节出题
                List<Chapter> allChapters = chapterService.findBySubject(subject);
                int midpoint = allChapters.size() / 2;
                List<Long> midtermChapterIds = allChapters.stream()
                        .limit(midpoint)
                        .map(Chapter::getId)
                        .collect(Collectors.toList());
                generateQuestionsByType(midtermChapterIds, counts, generatedQuestions);
                break;
                
            case "final":
                // 期末测试：从所有章节出题
                List<Chapter> chapters = chapterService.findBySubject(subject);
                List<Long> finalChapterIds = chapters.stream()
                        .map(Chapter::getId)
                        .collect(Collectors.toList());
                generateQuestionsByType(finalChapterIds, counts, generatedQuestions);
                break;
        }
        
        // 将生成的题目按题型分组，使用字符串作为键
        Map<String, List<Question>> questionsByType = new HashMap<>();
        
        // 按题型分组
        for (Question question : generatedQuestions) {
            String typeKey = question.getType().name();
            // 将SINGLE_CHOICE归类为CHOICE
            if ("SINGLE_CHOICE".equals(typeKey)) {
                typeKey = "CHOICE";
            }
            if (!questionsByType.containsKey(typeKey)) {
                questionsByType.put(typeKey, new ArrayList<>());
            }
            questionsByType.get(typeKey).add(question);
        }
        
        model.addAttribute("questionsByType", questionsByType);
        model.addAttribute("allQuestions", generatedQuestions);
        model.addAttribute("subject", subject);
        model.addAttribute("examType", examType);
        model.addAttribute("examName", examName);
        
        // 添加班级信息
        List<ClassEntity> targetClasses = new ArrayList<>();
        if (classIds != null && !classIds.isEmpty()) {
            targetClasses = classService.findAllById(classIds);
        }
        model.addAttribute("targetClasses", targetClasses);
        model.addAttribute("classIds", classIds);
        
        return "generated_exam";
    }
    
    @PostMapping("/save-exam")
    public String saveGeneratedExam(
            @RequestParam String examName,
            @RequestParam Long subjectId,
            @RequestParam String examType,
            @RequestParam(required = false) List<Long> questionIds,
            @RequestParam(required = false) List<Long> classIds) {
        
        // 创建新考试
        Exam exam = new Exam();
        exam.setName(examName);
        exam.setExamType(examType);
        exam.setExamDate(new Date()); // 默认为当前日期
        exam.setStatus("未开始");
        exam.setCreateTime(new Date());
        
        // 设置学科
        Subject subject = subjectService.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("无效的学科ID: " + subjectId));
        exam.setSubject(subject);
        
        // 设置目标班级
        if (classIds != null && !classIds.isEmpty()) {
            List<ClassEntity> targetClasses = classService.findAllById(classIds);
            exam.setTargetClasses(targetClasses);
        }
        
        // 设置题目
        if (questionIds != null && !questionIds.isEmpty()) {
            List<Question> questions = questionService.findAllById(questionIds);
            exam.setQuestions(questions);
            
            // 计算总分
            int totalScore = 100; // 默认总分
            exam.setTotalScore(totalScore);
        }
        
        // 保存考试
        Exam savedExam = examService.save(exam);
        
        return "redirect:/exams/" + savedExam.getId();
    }
    
    private void generateQuestionsByType(List<Long> chapterIds, Map<String, String> counts, List<Question> generatedQuestions) {
        // 定义所有支持的题型
        String[] questionTypes = {"CHOICE", "FILL_BLANK", "TRUE_FALSE", "SHORT_ANSWER"};
        
        // 从每种题型中抽取指定数量的题目
        for (String type : questionTypes) {
            String countKey = "count_" + type;
            if (counts.containsKey(countKey)) {
                try {
                    int count = Integer.parseInt(counts.get(countKey));
                    if (count > 0) {
                        try {
                            List<Question> questions = questionService.findRandomQuestionsByChapterIn(chapterIds, count * 2); // 获取更多题目以便筛选
                            List<Question> filteredQuestions;
                            
                            // 对CHOICE类型特殊处理
                            if ("CHOICE".equals(type)) {
                                // 筛选SINGLE_CHOICE和MULTIPLE_CHOICE题型
                                filteredQuestions = questions.stream()
                                    .filter(q -> 
                                        q.getType() == QuestionType.SINGLE_CHOICE || 
                                        q.getType() == QuestionType.MULTIPLE_CHOICE)
                                    .limit(count)
                                    .collect(Collectors.toList());
                            } else {
                                // 其他题型正常筛选
                                // FILL_BLANK 是旧写法，valueOf 不认识，用 parse 兼容别名
                                QuestionType questionTypeEnum = QuestionType.parse(type)
                                        .orElseThrow(IllegalArgumentException::new);
                                filteredQuestions = questions.stream()
                                    .filter(q -> q.getType() == questionTypeEnum)
                                    .limit(count)
                                    .collect(Collectors.toList());
                            }
                            
                            generatedQuestions.addAll(filteredQuestions);
                        } catch (IllegalArgumentException e) {
                            // 忽略无效的题型
                        }
                    }
                } catch (NumberFormatException e) {
                    // 忽略无效的数字
                }
            }
        }
    }
} 