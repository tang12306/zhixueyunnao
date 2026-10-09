package com._1.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 题型。数据库和接口里存枚举名，界面、提示词和导出文档里用中文名。
 */
public enum QuestionType {
    SINGLE_CHOICE("单选题"),
    MULTIPLE_CHOICE("多选题"),
    TRUE_FALSE("判断题"),
    FILL_IN_THE_BLANK("填空题"),
    SHORT_ANSWER("简答题");

    /** 旧代码、旧模板和模型输出里出现过的写法 */
    private static final Map<String, QuestionType> ALIASES = Map.of(
            "FILL_BLANK", FILL_IN_THE_BLANK,
            "CHOICE", SINGLE_CHOICE,
            "选择题", SINGLE_CHOICE,
            "TRUE_OR_FALSE", TRUE_FALSE,
            "JUDGE", TRUE_FALSE,
            "问答题", SHORT_ANSWER);

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isChoice() {
        return this == SINGLE_CHOICE || this == MULTIPLE_CHOICE;
    }

    /**
     * 接受枚举名（不区分大小写）、中文名和常见别名，无法识别时返回 empty。
     */
    public static Optional<QuestionType> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        String normalized = trimmed.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (QuestionType type : values()) {
            if (type.name().equals(normalized) || type.label.equals(trimmed)) {
                return Optional.of(type);
            }
        }
        QuestionType alias = ALIASES.get(normalized);
        return Optional.ofNullable(alias != null ? alias : ALIASES.get(trimmed));
    }

    /**
     * JSON 反序列化同样接受中文名和别名；空字符串当作没填。
     */
    @JsonCreator
    public static QuestionType fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return parse(value).orElseThrow(() -> new IllegalArgumentException("未知题型: " + value));
    }
}
