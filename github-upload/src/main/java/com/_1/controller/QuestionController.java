package com._1.controller;

import com._1.entity.Chapter;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.service.ChapterService;
import com._1.service.QuestionService;
import com._1.service.SubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/questions")
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    @Autowired
    private QuestionService questionService;

    @Autowired
    private SubjectService subjectService;
    
    @Autowired
    private ChapterService chapterService;

    @GetMapping
    public String listQuestions(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            Model model) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Question> questions;
        
        if (chapterId != null) {
            Chapter chapter = chapterService.findById(chapterId).orElse(null);
            if (chapter != null) {
                if (type != null && !type.isEmpty()) {
                    try {
                        QuestionType questionType = QuestionType.valueOf(type.toUpperCase());
                        questions = questionService.findAllByChapterAndType(chapter, questionType, pageRequest);
                    } catch (IllegalArgumentException e) {
                        questions = questionService.findAllByChapter(chapter, pageRequest);
                    }
                } else {
                    questions = questionService.findAllByChapter(chapter, pageRequest);
                }
            } else {
                questions = questionService.findAll(pageRequest);
            }
        } else if (subjectId != null) {
            Subject subject = subjectService.findById(subjectId).orElse(null);
            if (subject != null) {
                if (type != null && !type.isEmpty()) {
                    try {
                        QuestionType questionType = QuestionType.valueOf(type.toUpperCase());
                        questions = questionService.findAllBySubjectAndType(subject, questionType, pageRequest);
                    } catch (IllegalArgumentException e) {
                        questions = questionService.findAllBySubject(subject, pageRequest);
                    }
                } else {
                    questions = questionService.findAllBySubject(subject, pageRequest);
                }
            } else {
                questions = questionService.findAll(pageRequest);
            }
        } else if (type != null && !type.isEmpty()) {
            try {
                QuestionType questionType = QuestionType.valueOf(type.toUpperCase());
                questions = questionService.findAllByType(questionType, pageRequest);
            } catch (IllegalArgumentException e) {
                questions = questionService.findAll(pageRequest);
            }
        } else if (keyword != null && !keyword.trim().isEmpty()) {
            questions = questionService.search(keyword, pageRequest);
        } else {
            questions = questionService.findAll(pageRequest);
        }
        
        model.addAttribute("questions", questions);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", questions.getTotalPages());
        model.addAttribute("totalItems", questions.getTotalElements());
        
        List<Subject> subjects = subjectService.findAll();
        model.addAttribute("subjects", subjects);
        
        logger.info("Loaded {} subjects", subjects.size());
        for (Subject subj : subjects) {
            logger.info("Subject: ID={}, Name={}", 
                subj != null ? subj.getId() : "null_id", 
                subj != null && subj.getName() != null ? subj.getName() : "null_name");
        }
        
        if (subjectId != null) {
            Subject selectedSubject = subjectService.findById(subjectId).orElse(null);
            model.addAttribute("selectedSubject", selectedSubject);
            
            if (selectedSubject != null) {
                List<Chapter> chapters = chapterService.findBySubject(selectedSubject);
                model.addAttribute("chapters", chapters);
                logger.info("For subject '{}', loaded {} chapters", 
                    selectedSubject.getName() != null ? selectedSubject.getName() : "null_name", 
                    chapters.size());
                for (Chapter chapter : chapters) {
                    logger.info("Chapter: ID={}, Name={}, OrderNum={}", 
                              chapter != null ? chapter.getId() : "null_id", 
                              chapter != null && chapter.getName() != null ? chapter.getName() : "null_name", 
                              chapter != null ? chapter.getOrderNum() : "null_ordernum");
                }
            } else {
                model.addAttribute("chapters", new ArrayList<>());
            }
        } else {
            model.addAttribute("chapters", new ArrayList<>());
        }
        
        if (chapterId != null) {
            Chapter selectedChapter = chapterService.findById(chapterId).orElse(null);
            model.addAttribute("selectedChapter", selectedChapter);
            if (selectedChapter != null) {
                logger.info("Selected chapter: ID={}, Name={}", 
                    selectedChapter.getId(), 
                    selectedChapter.getName() != null ? selectedChapter.getName() : "null_name");
            }
        }
        
        model.addAttribute("questionTypes", Arrays.asList(QuestionType.values()));
        model.addAttribute("selectedType", type);
        
        return "questions";
    }

    @GetMapping("/new")
    public String newQuestionForm(Model model) {
        model.addAttribute("question", new Question());
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("questionTypes", Arrays.asList(QuestionType.values()));
        return "question_form";
    }

    // REST API: 根据ID获取题目详情
    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        try {
            Optional<Question> questionOptional = questionService.findById(id);
            if (questionOptional.isPresent()) {
                return ResponseEntity.ok(questionOptional.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching question with id: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/edit/{id}")
    public String editQuestionForm(@PathVariable Long id, Model model) {
        Question question = questionService.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid question Id:" + id));
        model.addAttribute("question", question);
        model.addAttribute("subjects", subjectService.findAll());
        
        Chapter questionChapter = question.getChapter();
        if (questionChapter != null && questionChapter.getSubject() != null) {
            Subject subject = questionChapter.getSubject();
            List<Chapter> chapters = chapterService.findBySubject(subject);
            model.addAttribute("chapters", chapters);
        }
        
        model.addAttribute("questionTypes", Arrays.asList(QuestionType.values()));
        model.addAttribute("optionsString", question.getOptions() != null ? String.join("\n", question.getOptions()) : "");
        return "question_form";
    }

    @PostMapping
    public String saveQuestion(@ModelAttribute Question question, 
                             @RequestParam(required = false) Long chapterId, 
                             @RequestParam(required = false) String optionsString, 
                             RedirectAttributes redirectAttributes) {
        try {
            logger.info("Saving/Updating question: ID={}, Title={}", question.getId(), question.getTitle());
            
            if (optionsString != null) {
                List<String> options = Arrays.stream(optionsString.split("\n"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                question.setOptions(options);
            }
            
            if (chapterId != null) {
                Chapter chapter = chapterService.findById(chapterId).orElse(null);
                question.setChapter(chapter);
                if (chapter != null && chapter.getSubject() != null && chapter.getSubject().getName() != null) {
                    question.setSubject(chapter.getSubject().getName());
                    logger.info("Set chapter {} for question.", chapter.getName());
                } else if (chapter != null) {
                     logger.warn("Chapter {} found, but its subject or subject name is null. Subject string for question might be incorrect.", chapter.getName());
                } else {
                    logger.warn("Chapter ID {} provided, but chapter not found. Question will have no chapter.", chapterId);
                }
            } else if (question.getChapter() != null && question.getChapter().getSubject() != null && question.getChapter().getSubject().getName() != null) {
                question.setSubject(question.getChapter().getSubject().getName());
            } else if (question.getSubject() == null || question.getSubject().trim().isEmpty()) {
                 logger.warn("Question subject string is null or empty, and no chapter information to derive it from.");
            }

            questionService.save(question);
            redirectAttributes.addFlashAttribute("successMessage", "题目保存成功!");
            logger.info("Question saved successfully: ID={}", question.getId());
        } catch (Exception e) {
            logger.error("Error saving question: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "保存题目失败: " + e.getMessage());
        }
        return "redirect:/questions";
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id) {
        try {
            questionService.deleteById(id);
            logger.info("Question with ID {} deleted successfully.", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error deleting question with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body("删除失败: " + e.getMessage());
        }
    }

    @PostMapping("/import")
    public String importQuestions(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        logger.warn("Excel import functionality has been disabled.");
        redirectAttributes.addFlashAttribute("warningMessage", "Excel导入功能已被禁用。");
        return "redirect:/questions";
    }

    @GetMapping("/api/query")
    @ResponseBody
    public ResponseEntity<Page<Question>> queryQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) String type, // e.g., "SINGLE_CHOICE"
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        logger.info("Querying questions with params: subjectId={}, chapterId={}, type={}, difficulty={}, keyword={}, page={}, size={}, sort={}",
                subjectId, chapterId, type, difficulty, keyword, page, size, sort);

        QuestionType questionType = null;
        if (type != null && !type.trim().isEmpty()) {
            try {
                questionType = QuestionType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid question type string provided: {}. It will be ignored.", type);
                // Optionally, you could return a BadRequest ResponseEntity here
                // return ResponseEntity.badRequest().body("Invalid question type: " + type);
            }
        }

        // Sorting
        String[] sortParams = sort.split(",");
        Sort.Direction direction = sortParams.length > 1 && "asc".equalsIgnoreCase(sortParams[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));

        try {
            Page<Question> questionPage = questionService.findByCriteria(
                    subjectId, chapterId, questionType, difficulty, keyword, pageRequest
            );
            return ResponseEntity.ok(questionPage);
        } catch (Exception e) {
            logger.error("Error querying questions: {}", e.getMessage(), e);
            // Consider returning a more specific error response if needed
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> exportQuestions(
        @RequestParam(required = false) Long subjectId,
        @RequestParam(required = false) Long chapterId) {
        logger.warn("Excel export functionality has been disabled.");
        return ResponseEntity.status(403).body(new ByteArrayResource("Excel导出功能已被禁用。".getBytes()));
    }

    @GetMapping("/chapters-by-subject")
    @ResponseBody
    public List<Chapter> getChaptersBySubject(@RequestParam Long subjectId) {
        Subject subject = subjectService.findById(subjectId).orElse(null);
        if (subject != null) {
            List<Chapter> chapters = chapterService.findBySubject(subject);
            logger.info("Fetching {} chapters for subject ID {}: ({})", chapters.size(), subjectId, chapters.stream().map(Chapter::getName).collect(Collectors.joining(", ")));
            return chapters;
        } else {
            logger.warn("Subject not found for ID {}, returning empty chapter list.", subjectId);
            return new ArrayList<>();
        }
    }

    @GetMapping("/export-template")
    public ResponseEntity<ByteArrayResource> exportTemplate() {
        logger.warn("Excel template export functionality has been disabled.");
        return ResponseEntity.status(403).body(new ByteArrayResource("Excel模板导出功能已被禁用。".getBytes()));
    }
    
    @PostMapping("/api/ai/save-questions")
    @ResponseBody
    public ResponseEntity<?> saveQuestions(@RequestBody List<Question> questions) {
        try {
            for(Question q : questions){
                if(q.getChapter() != null && q.getChapter().getName() != null && q.getSubject() != null){
                    Subject subject = subjectService.findByName(q.getSubject()).orElseGet(() -> {
                        Subject newSub = new Subject();
                        newSub.setName(q.getSubject());
                        return subjectService.save(newSub);
                    });
                    Chapter chapter = chapterService.findByNameAndSubject(q.getChapter().getName(), subject).orElseGet(()->{
                        Chapter newChap = new Chapter();
                        newChap.setName(q.getChapter().getName());
                        newChap.setSubject(subject);
                        return chapterService.save(newChap);
                    });
                    q.setChapter(chapter);
                }
            }
            questionService.saveAll(questions);
            return ResponseEntity.ok().body(Map.of("message", "Questions saved successfully"));
        } catch (Exception e) {
            logger.error("Error saving AI generated questions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error saving questions: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateQuestion(@PathVariable Long id, @RequestBody Question questionDetails) {
        try {
            Question existingQuestion = questionService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

            existingQuestion.setTitle(questionDetails.getTitle());
            existingQuestion.setSubject(questionDetails.getSubject());
            existingQuestion.setType(questionDetails.getType());
            existingQuestion.setDifficulty(questionDetails.getDifficulty());
            existingQuestion.setScore(questionDetails.getScore());
            existingQuestion.setContent(questionDetails.getContent());
            existingQuestion.setOptions(questionDetails.getOptions());
            existingQuestion.setAnswer(questionDetails.getAnswer());
            existingQuestion.setAnalysis(questionDetails.getAnalysis());
            existingQuestion.setTags(questionDetails.getTags());

            if (questionDetails.getChapter() != null && questionDetails.getChapter().getId() != null) {
                Chapter chapter = chapterService.findById(questionDetails.getChapter().getId()).orElse(null);
                existingQuestion.setChapter(chapter);
                 if (chapter != null && chapter.getSubject() != null && chapter.getSubject().getName() != null) {
                    existingQuestion.setSubject(chapter.getSubject().getName());
                }
            }
            
            Question updatedQuestion = questionService.save(existingQuestion);
            return ResponseEntity.ok(updatedQuestion);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating question with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error updating question: " + e.getMessage());
        }
    }
} 