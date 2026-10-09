package com._1.service.ai;

import com._1.entity.QuestionType;

import java.util.List;

/**
 * 还没入库的题目：AI 生成后返回给前端，教师编辑后再提交保存或导出。
 * options 只存选项内容，不带 "A." 前缀，显示时再按顺序加字母。
 */
public record QuestionDraft(
        QuestionType type,
        String content,
        List<String> options,
        String answer,
        String analysis,
        Integer score) {

    public QuestionDraft withScore(Integer newScore) {
        return new QuestionDraft(type, content, options, answer, analysis, newScore);
    }
}
