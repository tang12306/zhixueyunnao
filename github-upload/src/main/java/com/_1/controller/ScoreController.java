package com._1.controller;

import com._1.entity.Exam;
import com._1.entity.Score;
import com._1.service.ExamService;
import com._1.service.ScoreService;
// import com._1.service.ScoreExcelService; // 已移除
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.DoubleSummaryStatistics;

@Controller
@RequestMapping("/scores")
public class ScoreController {
    @Autowired
    private ScoreService scoreService;
    @Autowired
    private ExamService examService;
    // private ScoreExcelService scoreExcelService; // 已移除

    @GetMapping
    public String listScores(@RequestParam(value = "examId", required = false) Long examId, Model model) {
        List<Exam> exams = examService.findAll();
        model.addAttribute("exams", exams);
        List<Score> scores;
        if (examId != null) {
            Exam exam = examService.findById(examId).orElse(null);
            scores = exam != null ? scoreService.findByExam(exam) : List.of();
            model.addAttribute("selectedExamId", examId);
        } else {
            scores = scoreService.findAll();
        }
        model.addAttribute("scores", scores);
        // 统计信息
        if (!scores.isEmpty()) {
            DoubleSummaryStatistics stats = scores.stream()
                                                .filter(s -> s != null && s.getScore() != null)
                                                .mapToDouble(Score::getScore)
                                                .summaryStatistics();
            long passCount = scores.stream().filter(s -> s != null && s.getScore() != null && s.getScore() >= 60).count();
            
            if (stats.getCount() > 0) {
                double passRate = (double) passCount / stats.getCount() * 100;
                model.addAttribute("passRate", String.format("%.2f", passRate));
                model.addAttribute("avgScore", String.format("%.2f", stats.getAverage()));
                model.addAttribute("maxScore", stats.getMax());
                model.addAttribute("minScore", stats.getMin());
            } else {
                model.addAttribute("passRate", "N/A");
                model.addAttribute("avgScore", "N/A");
                model.addAttribute("maxScore", "N/A");
                model.addAttribute("minScore", "N/A");
            }
        }
        return "scores";
    }

    @GetMapping("/new")
    public String newScoreForm(Model model) {
        model.addAttribute("score", new Score());
        model.addAttribute("exams", examService.findAll());
        return "score_form";
    }

    @PostMapping
    public String saveScore(@ModelAttribute Score score) {
        scoreService.save(score);
        if (score != null && score.getExam() != null && score.getExam().getId() != null) {
            return "redirect:/scores?examId=" + score.getExam().getId();
        }
        return "redirect:/scores";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public void deleteScore(@PathVariable Long id) {
        scoreService.deleteById(id);
    }

    /* // 开始注释 exportScores 方法
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportScores(@RequestParam(value = "examId", required = false) Long examId) {
        List<Score> scores;
        if (examId != null) {
            Exam exam = examService.findById(examId).orElse(null);
            scores = exam != null ? scoreService.findByExam(exam) : List.of();
        } else {
            scores = scoreService.findAll();
        }
        // byte[] excelFile = scoreExcelService.exportScores(scores); // 此行依赖已删除服务
        // ByteArrayResource resource = new ByteArrayResource(excelFile);
        // return ResponseEntity.ok()
        //         .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=scores.xlsx")
        //         .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        //         .contentLength(excelFile.length)
        //         .body(resource);
        return ResponseEntity.noContent().build(); // 示例：返回 204 No Content
    }
    */ // 结束注释 exportScores 方法
} 