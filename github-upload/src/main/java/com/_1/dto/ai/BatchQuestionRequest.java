package com._1.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * AI 按题型批量出题（POST /api/ai/generate-batch-questions）。
 * type 接受枚举名（SINGLE_CHOICE）或中文名（单选题）。
 */
@Data
public class BatchQuestionRequest {

    @NotNull(message = "请选择学科")
    private Long subjectId;

    @Size(max = 50, message = "最多选择 50 个章节")
    private List<Long> chapterIds;

    @NotBlank(message = "请选择题型")
    private String type;

    @Min(value = 1, message = "难度为 1～5")
    @Max(value = 5, message = "难度为 1～5")
    private Integer difficulty;

    @NotNull(message = "请填写题目数量")
    @Min(value = 1, message = "题目数量为 1～50")
    @Max(value = 50, message = "题目数量为 1～50")
    private Integer count;

    @Size(max = 1000, message = "补充要求不能超过 1000 字")
    private String customPrompt;
}
