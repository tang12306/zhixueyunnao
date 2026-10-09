package com._1.controller;

import com._1.entity.Exam;
import com._1.entity.Question;
import com._1.service.ExamService;
import com._1.service.QuestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/exam-management")
public class ExamsController {
    private final ExamService examService;
    private final QuestionService questionService;

    public ExamsController(ExamService examService, QuestionService questionService) {
        this.examService = examService;
        this.questionService = questionService;
    }

    @GetMapping
    public String exams(Model model) {
        model.addAttribute("exams", examService.findAll());
        return "exams";
    }

    @GetMapping("/new")
    public String newExamForm(Model model) {
        model.addAttribute("exam", new Exam());
        return "exam_form";
    }

    @GetMapping("/edit/{id}")
    public String editExamForm(@PathVariable Long id, Model model) {
        Exam exam = examService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + id));
        model.addAttribute("exam", exam);
        return "exam_form";
    }

    @PostMapping
    public String saveExam(@ModelAttribute Exam exam) {
        examService.save(exam);
        return "redirect:/exam-management";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public void deleteExam(@PathVariable Long id) {
        examService.deleteById(id);
    }

    @GetMapping("/{id}/questions")
    public String selectQuestions(@PathVariable Long id, Model model) {
        Exam exam = examService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + id));
        model.addAttribute("exam", exam);
        model.addAttribute("questions", questionService.findAll());
        return "exam_questions";
    }

    @PostMapping("/{id}/questions")
    public String saveExamQuestions(@PathVariable Long id, @RequestParam(value = "questionIds", required = false) List<Long> questionIds) {
        Exam exam = examService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid exam Id:" + id));
        if (questionIds != null) {
            List<Question> questions = questionService.findAll().stream()
                .filter(q -> questionIds.contains(q.getId()))
                .toList();
            exam.setQuestions(questions);
        } else {
            exam.setQuestions(new java.util.ArrayList<>());
        }
        examService.save(exam);
        return "redirect:/exam-management";
    }
} 