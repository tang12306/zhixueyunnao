package com._1.service;

import java.util.List;
import java.util.Map;

public interface AIService {
    /**
     * 使用 AI 生成题目
     * @param subject 科目
     * @param type 题型
     * @param difficulty 难度
     * @param tags 标签
     * @param topic 主题
     * @param count 题目数量
     * @return 生成的题目列表
     */
    List<Map<String, Object>> generateQuestions(String subjectName, 
                                              String chapterName,
                                              String chapterDescription,
                                              String type, 
                                              Integer difficulty, List<String> tags, 
                                              String topic, Integer count);

    /**
     * 使用 AI 生成题目，支持自定义提示和分数生成。
     * @param subjectName 学科名称
     * @param chapterNames 章节名称列表 (可选)
     * @param chapterDescriptions 章节描述列表 (可选, 与chapterNames对应)
     * @param type 题型
     * @param difficulty 难度 (可选)
     * @param count 题目数量
     * @param customPrompt 用户自定义的附加提示 (可选)
     * @return 生成的题目列表，每个题目Map中应包含 "score" 字段
     */
    List<Map<String, Object>> generateQuestionsWithCustomPromptAndScore(
            String subjectName,
            List<String> chapterNames, // 改为列表以支持多章节
            List<String> chapterDescriptions, // 对应多章节描述
            String type,
            Integer difficulty,
            Integer count,
            String customPrompt // 新增自定义提示
    );

    /**
     * 使用 AI 优化题目
     * @param questionId 题目ID
     * @param instruction 优化指令
     * @return 优化后的题目
     */
    Map<String, Object> improveQuestion(Long questionId, String instruction);
} 