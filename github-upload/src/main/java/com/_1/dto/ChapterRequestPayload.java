package com._1.dto;

import lombok.Data;

@Data
public class ChapterRequestPayload {
    private String name;
    private String description;
    private Integer orderNum; // 可选，如果未提供，后端可自动生成
    private Long subjectId; // 关联的科目ID
} 