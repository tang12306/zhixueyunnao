package com._1.service.ai;

import com._1.entity.QuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 统一题目选项和答案的写法。题库里的选项只存内容，答案用字母表示（多选用英文逗号分隔），
 * 判断题答案用“正确/错误”。
 */
public final class QuestionTextNormalizer {

    /** 匹配 "A. "、"A、"、"(A) "、"（A）"、"A:" 这类选项前缀 */
    private static final Pattern LETTER_PREFIX =
            Pattern.compile("^\\s*[(（]?\\s*([A-Za-z])\\s*(?:[)）]\\s*[.．、:：]?|[.．、:：])\\s*");

    private static final Set<String> TRUE_WORDS = Set.of("正确", "对", "是", "√", "✓", "TRUE", "T", "YES", "Y");
    private static final Set<String> FALSE_WORDS = Set.of("错误", "错", "否", "×", "✗", "FALSE", "F", "NO", "N");

    private QuestionTextNormalizer() {
    }

    /**
     * 只有当每个选项都按顺序带着 A、B、C… 前缀时才去掉，避免误删 "A型血" 这类正文。
     */
    public static List<String> stripLetterPrefixes(List<String> options) {
        if (options == null || options.isEmpty()) {
            return options == null ? List.of() : options;
        }
        List<String> stripped = new ArrayList<>(options.size());
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i) == null ? "" : options.get(i);
            Matcher matcher = LETTER_PREFIX.matcher(option);
            char expected = (char) ('A' + i);
            if (!matcher.find() || Character.toUpperCase(matcher.group(1).charAt(0)) != expected) {
                return trimAll(options);
            }
            stripped.add(option.substring(matcher.end()).trim());
        }
        return stripped;
    }

    private static List<String> trimAll(List<String> options) {
        List<String> result = new ArrayList<>(options.size());
        for (String option : options) {
            result.add(option == null ? "" : option.trim());
        }
        return result;
    }

    /**
     * 把各种答案写法统一成题库格式：选择题 "A" / "A,C"，判断题 "正确" / "错误"，其他题型原样去空白。
     */
    public static String normalizeAnswer(QuestionType type, String answer) {
        if (answer == null) {
            return "";
        }
        String trimmed = answer.trim();
        if (type == null || trimmed.isEmpty()) {
            return trimmed;
        }
        return switch (type) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> normalizeChoiceAnswer(trimmed);
            case TRUE_FALSE -> normalizeTrueFalse(trimmed);
            default -> trimmed;
        };
    }

    private static String normalizeChoiceAnswer(String answer) {
        // 只处理纯字母答案，例如 "[A, C]"、"A、C"、"AC"、"a c"；带文字说明的答案保持原样
        String letters = answer.replaceAll("[\\[\\]\"'\\s,，、;；/]", "").toUpperCase(Locale.ROOT);
        if (!letters.matches("[A-H]{1,8}")) {
            return answer;
        }
        return String.join(",", letters.chars().distinct().sorted()
                .mapToObj(c -> String.valueOf((char) c)).toList());
    }

    private static String normalizeTrueFalse(String answer) {
        String key = answer.replaceAll("[。.!！\\s]", "").toUpperCase(Locale.ROOT);
        if (TRUE_WORDS.contains(key)) {
            return "正确";
        }
        if (FALSE_WORDS.contains(key)) {
            return "错误";
        }
        return answer;
    }
}
