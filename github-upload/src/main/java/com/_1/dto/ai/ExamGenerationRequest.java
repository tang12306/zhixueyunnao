package com._1.dto.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 一键出卷（POST /api/ai/generate-exam）。
 * questionPlan 的键是题型，接受枚举名或中文名，例如 {"FILL_IN_THE_BLANK": {"count": 10, "scorePerQuestion": 3}}。
 */
@Data
public class ExamGenerationRequest {

    /** 一套试卷最多多少道题；再多模型一次输出不完 */
    public static final int MAX_TOTAL_QUESTIONS = 60;

    @NotBlank(message = "请填写考试名称")
    @Size(max = 100, message = "考试名称不能超过 100 字")
    private String name;

    @Size(max = 500, message = "考试描述不能超过 500 字")
    private String description;

    @Min(value = 1, message = "考试时长为 1～600 分钟")
    @Max(value = 600, message = "考试时长为 1～600 分钟")
    private int duration;

    @Min(value = 1, message = "总分为 1～1000")
    @Max(value = 1000, message = "总分为 1～1000")
    private int targetScore;

    @NotNull(message = "请选择学科")
    private Long subjectId;

    @Size(max = 50, message = "最多选择 50 个章节")
    private List<Long> chapterIds = new ArrayList<>();

    @NotEmpty(message = "请设置题型和数量")
    private Map<String, @Valid QuestionPlan> questionPlan = new LinkedHashMap<>();

    @Size(max = 1000, message = "章节分布要求不能超过 1000 字")
    private String chapterDistribution;

    @Size(max = 1000, message = "特殊要求不能超过 1000 字")
    private String examRequirements;

    @Data
    public static class QuestionPlan {
        @Min(value = 0, message = "每种题型的数量为 0～50")
        @Max(value = 50, message = "每种题型的数量为 0～50")
        private int count;

        @Min(value = 0, message = "每题分值为 0～100")
        @Max(value = 100, message = "每题分值为 0～100")
        private int scorePerQuestion;
    }
}
