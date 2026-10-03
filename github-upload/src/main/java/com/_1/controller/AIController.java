package com._1.controller;

import com._1.entity.Chapter;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.service.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai")
public class AIController {
    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private AIService aiService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/generate-question")
    public ResponseEntity<?> generateSingleQuestion(
            @RequestParam Long subjectId,
            @RequestParam(required = false) Long chapterId,
            @RequestParam String type,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String topic,
            @RequestParam(defaultValue = "1") Integer count) {
        try {
            Subject subjectEntity = subjectService.findById(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + subjectId + " 的学科"));
            String subjectName = subjectEntity.getName();

            String chapterName = null;
            String chapterDescription = null;
            if (chapterId != null) {
                Chapter chapterEntity = chapterService.findById(chapterId)
                        .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + chapterId + " 的章节"));
                chapterName = chapterEntity.getName();
                chapterDescription = chapterEntity.getDescription();
            }
            List<Map<String, Object>> questions = aiService.generateQuestions(
                subjectName, chapterName, chapterDescription, type, difficulty, tags, topic, count);
            
            return ResponseEntity.ok(Map.of("success", true, "questions", questions));
        } catch (IllegalArgumentException e) {
             logger.warn("参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("AI生成题目时发生错误 - subjectId: {}, chapterId: {}", subjectId, chapterId, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "AI服务遇到问题：" + e.getMessage()));
        }
    }

    @PostMapping("/generate-batch-questions")
    public ResponseEntity<?> generateBatchQuestions(@RequestBody BatchQuestionRequestDTO requestDTO) {
        try {
            if (requestDTO.subjectId == null || requestDTO.type == null || requestDTO.count == null || requestDTO.count <= 0) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "学科ID、题型和题目数量不能为空且数量必须大于0"));
            }

            Subject subjectEntity = subjectService.findById(requestDTO.subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + requestDTO.subjectId + " 的学科"));
            String subjectName = subjectEntity.getName();

            List<String> chapterNames = new ArrayList<>();
            List<String> chapterDescriptions = new ArrayList<>();
            if (requestDTO.chapterIds != null && !requestDTO.chapterIds.isEmpty()) {
                for (Long chapId : requestDTO.chapterIds) {
                    Chapter chapterEntity = chapterService.findById(chapId)
                            .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + chapId + " 的章节"));
                    chapterNames.add(chapterEntity.getName());
                    chapterDescriptions.add(chapterEntity.getDescription());
                }
            }

            List<Map<String, Object>> questions = aiService.generateQuestionsWithCustomPromptAndScore(
                    subjectName,
                    chapterNames.isEmpty() ? null : chapterNames,
                    chapterDescriptions.isEmpty() ? null : chapterDescriptions,
                    requestDTO.type,
                    requestDTO.difficulty,
                    requestDTO.count,
                    requestDTO.customPrompt
            );
            return ResponseEntity.ok(Map.of("success", true, "questions", questions));
        } catch (IllegalArgumentException e) {
            logger.warn("批量生成题目参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("AI批量生成题目时发生严重错误: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "AI服务遇到问题：" + e.getMessage()));
        }
    }

    @PostMapping("/save-questions")
    public ResponseEntity<?> saveQuestions(@RequestBody List<Map<String, Object>> questionsDataList) {
        try {
            List<Question> savedQuestionsBatch = new ArrayList<>();
            
            for (Map<String, Object> questionData : questionsDataList) {
                Question question = new Question();

                Long subjectId = getLongFromMap(questionData, "subjectId");
                if (subjectId != null) {
                    Subject subjectEntity = subjectService.findById(subjectId)
                            .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + subjectId + " 的学科用于保存题目"));
                    question.setSubject(subjectEntity.getName());
                } else {
                    logger.warn("请求中未提供 subjectId，题目将不包含学科信息。原始数据: {}", questionData.get("subject"));
                    if (questionData.containsKey("subject") && questionData.get("subject") instanceof String) {
                         question.setSubject((String) questionData.get("subject"));
                    }
                }

                Long chapterId = getLongFromMap(questionData, "chapterId");
                if (chapterId != null) {
                    Chapter chapterEntity = chapterService.findById(chapterId)
                            .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + chapterId + " 的章节用于保存题目"));
                    question.setChapter(chapterEntity);
                }
                
                String typeString = (String) questionData.get("type");
                question.setType(mapChineseTypeToEnum(typeString));

                Integer difficultyToSet = getIntegerFromMap(questionData, "difficulty");
                question.setDifficulty(difficultyToSet != null ? difficultyToSet : 2);
                
                Integer scoreToSet = getIntegerFromMap(questionData, "score");
                question.setScore(scoreToSet);
                                
                question.setContent((String) questionData.get("content"));
                
                Object optionsObject = questionData.get("options");
                if (optionsObject instanceof String) {
                    try {
                        List<String> optionsList = objectMapper.readValue((String) optionsObject, new TypeReference<List<String>>() {});
                        question.setOptions(optionsList);
                    } catch (Exception e) {
                        logger.warn("解析题目选项JSON失败: {}。原始数据: {}", e.getMessage(), optionsObject);
                        question.setOptions(new ArrayList<>());
                    }
                } else if (optionsObject instanceof List) {
                     try {
                        @SuppressWarnings("unchecked")
                        List<String> directOptionsList = (List<String>) optionsObject;
                        question.setOptions(directOptionsList);
                    } catch (ClassCastException cce) {
                        logger.warn("选项直接作为List提供，但元素类型不正确: {}", cce.getMessage());
                        question.setOptions(new ArrayList<>());
                    }
                }

                // 处理答案数据 - 可能是String或List
                Object answerObject = questionData.get("answer");
                if (answerObject instanceof String) {
                    question.setAnswer((String) answerObject);
                } else if (answerObject instanceof List) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> answerList = (List<String>) answerObject;
                        // 将List转换为逗号分隔的字符串
                        question.setAnswer(String.join(",", answerList));
                    } catch (ClassCastException cce) {
                        logger.warn("答案作为List提供，但元素类型不正确: {}", cce.getMessage());
                        question.setAnswer("");
                    }
                } else if (answerObject != null) {
                    // 其他类型转换为字符串
                    question.setAnswer(answerObject.toString());
                } else {
                    question.setAnswer("");
                }

                // 处理解析数据
                Object analysisObject = questionData.get("analysis");
                if (analysisObject instanceof String) {
                    question.setAnalysis((String) analysisObject);
                } else if (analysisObject != null) {
                    question.setAnalysis(analysisObject.toString());
                } else {
                    question.setAnalysis("");
                }
                
                Object tagsObject = questionData.get("tags");
                 if (tagsObject instanceof List) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> directTagsList = (List<String>) tagsObject;
                        question.setTags(directTagsList);
                    } catch (ClassCastException cce) {
                         logger.warn("标签作为List提供，但元素类型不正确: {}", cce.getMessage());
                    }
                }
                savedQuestionsBatch.add(question);
            }
            
            List<Question> result = questionService.saveAll(savedQuestionsBatch);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "成功保存 " + result.size() + " 道题目",
                "count", result.size()
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("保存题目参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
        catch (Exception e) {
            logger.error("保存题目失败", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "保存题目时发生内部错误：" + e.getMessage()));
        }
    }

    private Long getLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                logger.warn("无法将字符串 '{}' 解析为Long类型，对应键: {}", value, key);
                return null;
            }
        }
        return null;
    }
    
    private Integer getIntegerFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
         if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                logger.warn("无法将字符串 '{}' 解析为Integer类型，对应键: {}", value, key);
                return null;
            }
        }
        return null;
    }

    private QuestionType mapChineseTypeToEnum(String chineseType) {
        if (chineseType == null || chineseType.trim().isEmpty()) {
            logger.warn("中文题型为空，默认为单选题");
            return QuestionType.SINGLE_CHOICE;
        }
        switch (chineseType) {
            case "单选题":
                return QuestionType.SINGLE_CHOICE;
            case "多选题":
                return QuestionType.MULTIPLE_CHOICE;
            case "判断题":
                return QuestionType.TRUE_FALSE;
            case "填空题":
                return QuestionType.FILL_IN_THE_BLANK;
            case "简答题":
                return QuestionType.SHORT_ANSWER;
            default:
                logger.warn("未知的中文题型: {}，默认为单选题", chineseType);
                return QuestionType.SINGLE_CHOICE;
        }
    }

    @PostMapping("/improve-question")
    public ResponseEntity<?> improveQuestion(
            @RequestParam Long questionId,
            @RequestParam String instruction) {
        try {
            Map<String, Object> improvedQuestion = aiService.improveQuestion(questionId, instruction);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "question", improvedQuestion
            ));
        } catch (Exception e) {
            logger.error("优化题目失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }

    static class BatchQuestionRequestDTO {
        public Long subjectId;
        public List<Long> chapterIds;
        public String type;
        public Integer difficulty;
        public Integer count;
        public String customPrompt;
    }
} 