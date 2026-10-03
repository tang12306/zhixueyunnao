package com._1.controller;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Question;
import com._1.entity.Subject;
import com._1.service.ClassService;
import com._1.service.ExamService;
import com._1.service.QuestionService;
import com._1.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/exams")
public class ExamManagementController {

    private static final Logger logger = LoggerFactory.getLogger(ExamManagementController.class);

    @Autowired
    private ExamService examService;
    
    @Autowired
    private SubjectService subjectService;
    
    @Autowired
    private ClassService classService;
    
    @Autowired
    private QuestionService questionService;
    
    // 考试列表
    @GetMapping
    public String listExams(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "examDate", "createTime"));
            Page<Exam> examsPage;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
                examsPage = examService.search(keyword, pageable);
        } else if (subjectId != null) {
            Subject subject = subjectService.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("无效的学科ID: " + subjectId));
                // examService.findBySubject(subject, pageable) // 假设 ExamService 支持分页查询
                // 为了简化，如果findBySubject不支持分页，先获取列表再手动分页 (不推荐用于大量数据)
            List<Exam> subjectExams = examService.findBySubject(subject);
                 // 手动分页处理 (如果service层不支持)
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), subjectExams.size());
                List<Exam> pagedList = subjectExams.subList(start, end);
                examsPage = new org.springframework.data.domain.PageImpl<>(pagedList, pageable, subjectExams.size());

        } else {
                examsPage = examService.findAll(pageable);
        }
        
            model.addAttribute("exams", examsPage);
        model.addAttribute("keyword", keyword);
            model.addAttribute("selectedSubjectId", subjectId);
            model.addAttribute("subjects", subjectService.findAll());
            model.addAttribute("currentPage", examsPage.getNumber());
            model.addAttribute("totalPages", examsPage.getTotalPages());
            model.addAttribute("totalItems", examsPage.getTotalElements());
        } catch (Exception e) {
            logger.error("获取考试列表失败", e);
            model.addAttribute("errorMessage", "加载考试列表失败: " + e.getMessage());
            // 返回一个错误页面或带有错误消息的当前页面
        }
        return "exam/exam_list"; // 视图名称，后续创建
    }
    
    // 考试详情
    @GetMapping("/{id}")
    public String viewExam(@PathVariable Long id, Model model) {
        try {
        Exam exam = examService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("无效的考试ID: " + id));
        model.addAttribute("exam", exam);
            // model.addAttribute("targetClasses", exam.getTargetClasses()); // 假设 Exam 实体已加载
            // model.addAttribute("questions", exam.getQuestions());       // 假设 Exam 实体已加载
        } catch (Exception e) {
            logger.error("查看考试详情失败, ID: {}", id, e);
            model.addAttribute("errorMessage", "加载考试详情失败: " + e.getMessage());
        }
        return "exam/exam_detail"; // 视图名称，后续创建
    }

    // 用于从出题系统自动保存考试的接口
    @PostMapping("/from-generator")
    public ResponseEntity<?> saveExamFromGenerator(@RequestBody ExamGenerationRequest request) {
        try {
            Exam exam = new Exam();
            exam.setName(request.getExamName());
            exam.setExamType(request.getExamType());
            exam.setExamDate(new Date()); // 默认当前日期，或从request获取
            exam.setStatus("未开始");      // 默认状态
            exam.setCreateTime(new Date());

            Subject subject = subjectService.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("无效的学科ID: " + request.getSubjectId()));
            exam.setSubject(subject);

            if (request.getClassIds() != null && !request.getClassIds().isEmpty()) {
                List<ClassEntity> targetClasses = request.getClassIds().stream()
                    .map(classId -> classService.findById(classId).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                exam.setTargetClasses(targetClasses);
            }

            if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
                List<Question> questions = request.getQuestionIds().stream()
                    .map(questionId -> questionService.findById(questionId).orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                exam.setQuestions(questions);
                // 计算总分逻辑，这里简化为题目数量*默认分值，或从Question实体获取
                int totalScore = questions.stream().mapToInt(q -> q.getScore() != null ? q.getScore() : 10).sum(); // 假设默认每题10分
                exam.setTotalScore(totalScore);
            } else {
                exam.setTotalScore(0);
            }

            Exam savedExam = examService.save(exam);
            // 返回新创建的考试的ID或整个对象
            return ResponseEntity.status(HttpStatus.CREATED).body(savedExam);
        } catch (IllegalArgumentException e) {
            logger.error("从生成器保存考试失败 (参数错误): {}", e.getMessage());
            return ResponseEntity.badRequest().body("参数错误: " + e.getMessage());
        } catch (Exception e) {
            logger.error("从生成器保存考试失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("保存考试失败: " + e.getMessage());
        }
    }
    
    // 显示创建新考试的表单 (如果除了自动生成，还允许手动创建)
    @GetMapping("/new")
    public String newExamForm(Model model) {
        model.addAttribute("exam", new Exam());
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("classes", classService.findAll()); // 用于选择目标班级
        model.addAttribute("allQuestions", questionService.findAll()); // 用于选择题目，可能需要分页
        return "exam/exam_form"; // 视图名称，后续创建
    }
    
    // 保存手动创建或编辑的考试
    @PostMapping("/save")
    public String saveOrUpdateExam(@ModelAttribute Exam exam, 
            @RequestParam Long subjectId,
            @RequestParam(required = false) List<Long> classIds,
                               @RequestParam(required = false) List<Long> questionIds,
                               Model model) {
        try {
        Subject subject = subjectService.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("无效的学科ID: " + subjectId));
        exam.setSubject(subject);
        
            if (classIds != null) {
            List<ClassEntity> targetClasses = classIds.stream()
                .map(id -> classService.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            exam.setTargetClasses(targetClasses);
        }
        
            if (questionIds != null) {
            List<Question> questions = questionIds.stream()
                .map(id -> questionService.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            exam.setQuestions(questions);
                int totalScore = questions.stream().mapToInt(q -> q.getScore() != null ? q.getScore() : 10).sum();
            exam.setTotalScore(totalScore);
            } else if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
                 exam.setTotalScore(0);
            }


            if (exam.getId() == null) { // 新建
                exam.setCreateTime(new Date());
                exam.setStatus("未开始"); 
            }
             // examDate 等字段应在表单中设置
        examService.save(exam);
            return "redirect:/exams";
        } catch (Exception e) {
            logger.error("保存考试失败", e);
            model.addAttribute("errorMessage", "保存失败: " + e.getMessage());
            model.addAttribute("exam", exam); // 回填表单
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("classes", classService.findAll());
            model.addAttribute("allQuestions", questionService.findAll());
            return "exam/exam_form";
    }
    }

    // 显示编辑考试的表单
    @GetMapping("/edit/{id}")
    public String editExamForm(@PathVariable Long id, Model model) {
        try {
            Exam exam = examService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("无效的考试ID: " + id));
            model.addAttribute("exam", exam);
            model.addAttribute("subjects", subjectService.findAll());
            model.addAttribute("classes", classService.findAll());
            model.addAttribute("allQuestions", questionService.findAll()); //  用于增删题目
        } catch (Exception e) {
            logger.error("加载编辑考试表单失败, ID: {}", id, e);
            model.addAttribute("errorMessage", "加载考试信息失败: " + e.getMessage());
            return "redirect:/exams"; // 或者错误页
        }
        return "exam/exam_form";
    }
    
    // 删除考试
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExam(@PathVariable Long id) {
        try {
            examService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("删除考试失败, ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("删除失败: " + e.getMessage());
        }
    }
    }
    
// DTO for receiving exam generation requests
class ExamGenerationRequest {
    private String examName;
    private Long subjectId;
    private String examType;
    private List<Long> classIds;
    private List<Long> questionIds;
    // Getters and Setters
    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
    public List<Long> getClassIds() { return classIds; }
    public void setClassIds(List<Long> classIds) { this.classIds = classIds; }
    public List<Long> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Long> questionIds) { this.questionIds = questionIds; }
} 