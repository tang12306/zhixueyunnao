package com._1.dto.ai;

import lombok.Data;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 保存一道 AI 生成的题目到题库（POST /api/ai/save-questions 的数组元素）。
 * type 接受枚举名或中文名；answer 可以是字符串，也可以是数组（多选题），数组会用英文逗号拼接。
 */
@Data
public class SaveQuestionRequest {

    private Long subjectId;

    /** 旧页面只传学科名称，没有 subjectId 时使用 */
    private String subject;

    private Long chapterId;

    private String type;

    private Integer difficulty;

    private Integer score;

    private String content;

    private List<String> options;

    private String answer;

    private String analysis;

    private List<String> tags;

    public void setAnswer(Object answer) {
        if (answer == null) {
            this.answer = null;
        } else if (answer instanceof Collection<?> parts) {
            this.answer = parts.stream().map(String::valueOf).collect(Collectors.joining(","));
        } else {
            this.answer = String.valueOf(answer);
        }
    }
}
