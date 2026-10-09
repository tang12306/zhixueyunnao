package com._1.dto.ai;

import com._1.service.ai.QuestionDraft;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 把未入库的试卷直接导出为 Word（POST /api/ai/export/word）。
 */
@Data
public class ExamExportRequest {

    @NotBlank(message = "请填写试卷名称")
    @Size(max = 100, message = "试卷名称不能超过 100 字")
    private String name;

    @Size(max = 500, message = "试卷说明不能超过 500 字")
    private String description;

    private Integer duration;

    private Integer totalScore;

    @NotEmpty(message = "试卷里没有题目")
    @Size(max = 200, message = "一次最多导出 200 道题")
    private List<QuestionDraft> questions = new ArrayList<>();
}
