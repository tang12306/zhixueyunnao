package com._1.controller;

import com._1.entity.Exam;
import com._1.service.ExamService;
import com._1.service.ScoreService;
import com._1.service.UserService;
import com._1.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com._1.entity.ClassEntity;
import com._1.entity.Score;
import com._1.service.ClassService;
import com._1.service.SubjectService;

@Controller
@RequestMapping("/statistics")
public class StatisticsController {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    private final ExamService examService;
    private final ScoreService scoreService;
    private final ClassService classService;
    private final UserService userService;
    private final SubjectService subjectService;

    public StatisticsController(ExamService examService,
                                ScoreService scoreService,
                                ClassService classService,
                                UserService userService,
                                SubjectService subjectService) {
        this.examService = examService;
        this.scoreService = scoreService;
        this.classService = classService;
        this.userService = userService;
        this.subjectService = subjectService;
    }

    @GetMapping
    public String statistics(Model model) {
        var exams = examService.findAll();
        model.addAttribute("exams", exams);
        // 统计每场考试的平均分、最高分、最低分、及格率
        var statsList = exams.stream().map(exam -> {
            var scores = scoreService.findByExam(exam);
            DoubleSummaryStatistics stats = scores.stream().mapToDouble(s -> s.getScore()).summaryStatistics();
            long passCount = scores.stream().filter(s -> s.getScore() >= 60).count();
            double passRate = scores.isEmpty() ? 0 : (double) passCount / scores.size() * 100;
            return new Object[]{
                exam.getName(),
                scores.size(),
                String.format("%.2f", stats.getAverage()),
                stats.getMax(),
                stats.getMin(),
                String.format("%.2f", passRate)
            };
        }).toList();
        model.addAttribute("statsList", statsList);
        return "statistics";
    }

    // ==== 成绩导入 ====
    @GetMapping("/scores/import")
    public String showImportScoresForm(Model model) {
        try {
            model.addAttribute("exams", examService.findAll());
            model.addAttribute("classes", classService.findAll());
        } catch (Exception e) {
            logger.error("Error loading import scores form", e);
            model.addAttribute("errorMessage", "加载导入表单失败");
        }
        return "statistics/score_import_form";
    }

    @PostMapping("/scores/import")
    public String handleImportScores(
            @RequestParam("examId") Long examId,
            @RequestParam("classId") Long classId,
            @RequestParam("file") MultipartFile file,
            Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("errorMessage", "请选择要上传的Excel文件");
                model.addAttribute("exams", examService.findAll());
                model.addAttribute("classes", classService.findAll());
                return "statistics/score_import_form";
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                model.addAttribute("errorMessage", "请上传Excel格式的文件 (.xlsx或.xls)");
                model.addAttribute("exams", examService.findAll());
                model.addAttribute("classes", classService.findAll());
                return "statistics/score_import_form";
            }

            Exam exam = examService.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("无效的考试ID: " + examId));
            ClassEntity classEntity = classService.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("无效的班级ID: " + classId));

            List<Score> importedScores = scoreService.importScoresFromExcel(file, exam, classEntity);
            model.addAttribute("successMessage", "成功导入 " + importedScores.size() + " 条成绩记录 for " + classEntity.getName() + " - " + exam.getName());
            
        } catch (IllegalArgumentException e) {
            logger.warn("Argument error during score import: {}", e.getMessage());
            model.addAttribute("errorMessage", "导入失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error importing scores", e);
            model.addAttribute("errorMessage", "导入失败，发生内部错误");
        }
        model.addAttribute("exams", examService.findAll());
        model.addAttribute("classes", classService.findAll());
        return "statistics/score_import_form";
    }

    // ==== 班级均分对比 ====
    @GetMapping("/scores/class-comparison")
    public String showClassComparisonPage(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) List<Long> classIds,
            Model model) {
        try {
            model.addAttribute("exams", examService.findAll());
            model.addAttribute("allClasses", classService.findAll()); 

            if (examId != null && classIds != null && !classIds.isEmpty()) {
                Exam exam = examService.findById(examId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的考试ID: " + examId));
                
                Map<String, Object> rawAverageScores = scoreService.compareClassesAverageScores(classIds, examId);
                Map<String, Double> classAverageScores = new HashMap<>();
                if (rawAverageScores != null) {
                    for (Map.Entry<String, Object> entry : rawAverageScores.entrySet()) {
                        if (entry.getValue() instanceof Number) {
                            classAverageScores.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                        } else {
                            logger.warn("Score value for class '{}' is not a number: {}", entry.getKey(), entry.getValue());
                            // Optionally, put a default value or skip, e.g., classAverageScores.put(entry.getKey(), 0.0);
                        }
                    }
                }

                List<ClassEntity> selectedClasses = classService.findAllById(classIds);
                if (selectedClasses.size() != classIds.size()) {
                     logger.warn("Some class IDs provided for comparison were not found. Provided: {}, Found: {}", 
                                 classIds, selectedClasses.stream().map(ClassEntity::getId).collect(Collectors.toList()));
                }

                model.addAttribute("classAverageScores", classAverageScores);
                model.addAttribute("selectedExam", exam);
                model.addAttribute("selectedClasses", selectedClasses); 
                model.addAttribute("comparisonDataReady", true);
                logger.info("Generating class comparison for examId: {} and classIds: {}", examId, classIds);

            } else {
                model.addAttribute("comparisonDataReady", false);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Argument error during class comparison: {}", e.getMessage());
            model.addAttribute("errorMessage", "加载对比数据失败: " + e.getMessage());
            model.addAttribute("comparisonDataReady", false);
        } catch (Exception e) {
            logger.error("Error loading class comparison page", e);
            model.addAttribute("errorMessage", "加载班级均分对比页面失败");
            model.addAttribute("comparisonDataReady", false);
        }
        return "statistics/class_comparison";
    }

    // ==== 班级成绩分布 ====
    @GetMapping("/scores/class-distribution")
    public String showClassDistributionPage(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long classId,
            Model model) {
        try {
            model.addAttribute("exams", examService.findAll());
            model.addAttribute("allClasses", classService.findAll());

            if (examId != null && classId != null) {
                Exam exam = examService.findById(examId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的考试ID: " + examId));
                ClassEntity classEntity = classService.findById(classId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的班级ID: " + classId));

                Map<String, Integer> scoreDistribution = scoreService.getClassScoreDistribution(classId, examId);

                model.addAttribute("scoreDistribution", scoreDistribution);
                model.addAttribute("selectedExam", exam);
                model.addAttribute("selectedClass", classEntity);
                model.addAttribute("distributionDataReady", true);
                logger.info("Generating score distribution for examId: {} and classId: {}", examId, classId);
            } else {
                model.addAttribute("distributionDataReady", false);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Argument error during score distribution: {}", e.getMessage());
            model.addAttribute("errorMessage", "加载成绩分布数据失败: " + e.getMessage());
            model.addAttribute("distributionDataReady", false);
        } catch (Exception e) {
            logger.error("Error loading score distribution page", e);
            model.addAttribute("errorMessage", "加载班级成绩分布页面失败");
            model.addAttribute("distributionDataReady", false);
        }
        return "statistics/class_distribution";
    }

    // ==== 学生个人成绩分析 ====
    @GetMapping("/scores/student-analysis")
    public String showStudentAnalysisPage(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long subjectId, // Optional: filter by subject
            Model model) {
        try {
            List<User> allUsers = userService.findAll();
            List<User> allStudents = allUsers.stream().filter(u -> "STUDENT".equals(u.getRole())).collect(Collectors.toList());
            model.addAttribute("allStudents", allStudents);

            model.addAttribute("allSubjects", subjectService.findAll()); // For subject filter dropdown

            if (studentId != null) {
                User student = userService.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的学生ID: " + studentId));
                
                // analyzeStudentScoresTrend might take subjectId as nullable or 0 if no subject filter
                Map<String, Object> trendData = scoreService.analyzeStudentScoresTrend(studentId, subjectId);

                model.addAttribute("trendData", trendData); // Contains examNames, scores, ranks, etc.
                model.addAttribute("selectedStudent", student);
                if (subjectId != null) {
                    subjectService.findById(subjectId).ifPresent(subject -> model.addAttribute("selectedSubject", subject));
                }
                model.addAttribute("analysisDataReady", true);
                logger.info("Generating student analysis for studentId: {} and subjectId: {}", studentId, subjectId);
            } else {
                model.addAttribute("analysisDataReady", false);
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Argument error during student analysis: {}", e.getMessage());
            model.addAttribute("errorMessage", "加载学生分析数据失败: " + e.getMessage());
            model.addAttribute("analysisDataReady", false);
        } catch (Exception e) {
            logger.error("Error loading student analysis page", e);
            model.addAttribute("errorMessage", "加载学生个人分析页面失败");
            model.addAttribute("analysisDataReady", false);
        }
        return "statistics/student_analysis";
    }
} 