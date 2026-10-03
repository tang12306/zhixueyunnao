package com._1.controller;

import com._1.entity.Chapter;
import com._1.entity.Subject;
import com._1.service.ChapterService;
import com._1.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chapters")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;
    
    @Autowired
    private SubjectService subjectService;

    @GetMapping
    public String listChapters(@RequestParam(required = false) Long subjectId, Model model) {
        List<Subject> subjects = subjectService.findAll();
        model.addAttribute("subjects", subjects);
        
        if (subjectId != null) {
            Subject subject = subjectService.findById(subjectId).orElse(null);
            if (subject != null) {
                List<Chapter> chapters = chapterService.findBySubject(subject);
                model.addAttribute("selectedSubject", subject);
                model.addAttribute("chapters", chapters);
            }
        }
        
        model.addAttribute("chapter", new Chapter());
        return "chapters";
    }

    @GetMapping("/api/by-subject")
    @ResponseBody
    public ResponseEntity<List<Chapter>> getChaptersBySubjectJson(@RequestParam Long subjectId) {
        Subject subject = subjectService.findById(subjectId).orElse(null);
        if (subject != null) {
            List<Chapter> chapters = chapterService.findBySubject(subject);
            return ResponseEntity.ok(chapters);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public String saveChapter(@ModelAttribute Chapter chapter, @RequestParam Long subjectId) {
        Subject subject = subjectService.findById(subjectId).orElse(null);
        if (subject != null) {
            chapter.setSubject(subject);
            chapterService.save(chapter);
        }
        return "redirect:/chapters?subjectId=" + subjectId;
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Chapter> getChapter(@PathVariable Long id) {
        return chapterService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteChapter(@PathVariable Long id) {
        try {
            Chapter chapter = chapterService.findById(id).orElse(null);
            Long subjectId = null;
            if (chapter != null && chapter.getSubject() != null) {
                subjectId = chapter.getSubject().getId();
            }
            chapterService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 