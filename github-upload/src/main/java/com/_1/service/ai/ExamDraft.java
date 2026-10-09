package com._1.service.ai;

import java.util.List;

/**
 * AI 生成的试卷草稿，不入库，交给前端预览、编辑、导出或把题目存进题库。
 *
 * @param totalScore 实际题目分值合计，可能与 targetScore 不同
 * @param warnings   题型数量与计划不符等提示
 */
public record ExamDraft(
        String name,
        String description,
        int duration,
        int targetScore,
        int totalScore,
        Long subjectId,
        String subjectName,
        List<QuestionDraft> questions,
        List<String> warnings) {
}
