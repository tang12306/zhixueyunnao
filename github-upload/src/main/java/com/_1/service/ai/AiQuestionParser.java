package com._1.service.ai;

import com._1.entity.QuestionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把模型回复解析成题目列表。
 * <p>
 * 兼容 {"questions":[...]} 和裸数组两种结构、```json 代码块、注释和多余逗号，
 * 选项既可以是字符串数组也可以是 {key, content} 对象。缺字段的题目会被跳过；
 * 一道题都解析不出来时抛出 {@link AiServiceException}，并带上原始回复，不再静默返回空列表。
 */
@Component
public class AiQuestionParser {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionParser.class);

    /** 原始回复最多带回多少字符给前端 */
    static final int RAW_LIMIT = 4000;

    private static final Pattern CODE_FENCE = Pattern.compile("```(?:json|JSON)?\\s*([\\s\\S]*?)```");

    private static final JsonMapper LENIENT = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    /**
     * @param raw          模型回复
     * @param defaultType  回复里没写题型或题型无法识别时使用；为 null 时按有无选项推断
     * @param defaultScore 回复里没写分值时使用
     * @param truncated    回复是否因长度上限被截断，只影响错误提示
     */
    public List<QuestionDraft> parse(String raw, QuestionType defaultType, int defaultScore, boolean truncated) {
        if (raw == null || raw.isBlank()) {
            throw AiServiceException.unparseable("AI 没有返回任何内容，请重试", "");
        }
        JsonNode root;
        try {
            root = LENIENT.readTree(extractJson(raw));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("AI 回复不是有效的 JSON（{} 字符，截断={}）", raw.length(), truncated);
            throw AiServiceException.unparseable(truncated
                    ? "AI 回复超过长度上限被截断，请减少题目数量后重试"
                    : "AI 返回的内容不是有效的 JSON，请重试", abbreviate(raw));
        }

        JsonNode items = findQuestionArray(root);
        if (items == null) {
            throw AiServiceException.unparseable("AI 返回的 JSON 里没有题目列表，请重试", abbreviate(raw));
        }

        List<QuestionDraft> questions = new ArrayList<>();
        int skipped = 0;
        for (JsonNode item : items) {
            QuestionDraft question = toQuestion(item, defaultType, defaultScore);
            if (question == null) {
                skipped++;
            } else {
                questions.add(question);
            }
        }
        if (skipped > 0) {
            log.warn("AI 回复中有 {} 道题缺少题干，已跳过", skipped);
        }
        if (questions.isEmpty()) {
            throw AiServiceException.unparseable("AI 返回的 JSON 里没有可用的题目，请重试", abbreviate(raw));
        }
        return questions;
    }

    /** 去掉代码块标记，截取第一个 { 或 [ 到最后一个 } 或 ] */
    static String extractJson(String raw) {
        String text = raw;
        Matcher fence = CODE_FENCE.matcher(raw);
        if (fence.find()) {
            text = fence.group(1);
        }
        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');
        int start;
        char closer;
        if (arrayStart >= 0 && (objectStart < 0 || arrayStart < objectStart)) {
            start = arrayStart;
            closer = ']';
        } else if (objectStart >= 0) {
            start = objectStart;
            closer = '}';
        } else {
            throw new IllegalArgumentException("回复中没有 JSON");
        }
        int end = text.lastIndexOf(closer);
        if (end <= start) {
            throw new IllegalArgumentException("JSON 不完整");
        }
        return text.substring(start, end + 1);
    }

    private static JsonNode findQuestionArray(JsonNode root) {
        if (root.isArray()) {
            return root;
        }
        if (!root.isObject()) {
            return null;
        }
        if (root.path("questions").isArray()) {
            return root.get("questions");
        }
        // 只有一个数组字段时（例如模型把 questions 写成了 items），就用它
        JsonNode onlyArray = null;
        for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
            JsonNode value = it.next().getValue();
            if (value.isArray()) {
                if (onlyArray != null) {
                    return null;
                }
                onlyArray = value;
            }
        }
        if (onlyArray != null) {
            return onlyArray;
        }
        // 只返回了一道题的对象
        return root.has("content") ? LENIENT.createArrayNode().add(root) : null;
    }

    private static QuestionDraft toQuestion(JsonNode node, QuestionType defaultType, int defaultScore) {
        if (!node.isObject()) {
            return null;
        }
        String content = text(node, "content", "question", "stem");
        if (content.isBlank()) {
            return null;
        }
        List<String> options = QuestionTextNormalizer.stripLetterPrefixes(options(node.get("options")));

        QuestionType type = QuestionType.parse(text(node, "type")).orElse(defaultType);
        if (type == null) {
            type = options.isEmpty() ? QuestionType.SHORT_ANSWER : QuestionType.SINGLE_CHOICE;
        }
        if (!type.isChoice()) {
            options = List.of();
        }

        String answer = QuestionTextNormalizer.normalizeAnswer(type, answer(node.get("answer")));
        String analysis = text(node, "analysis", "explanation");
        Integer score = score(node.get("score"), defaultScore);
        return new QuestionDraft(type, content.trim(), options, answer, analysis.trim(), score);
    }

    private static String text(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull() && !value.isContainerNode()) {
                String text = value.asText();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static List<String> options(JsonNode node) {
        List<String> options = new ArrayList<>();
        if (node == null || node.isNull()) {
            return options;
        }
        if (node.isArray()) {
            for (JsonNode option : node) {
                if (option.isObject()) {
                    // {"key":"A","content":"..."}，key 由显示时按顺序生成
                    String content = text(option, "content", "text", "value");
                    options.add(content);
                } else if (!option.isNull()) {
                    options.add(option.asText());
                }
            }
        } else if (node.isObject()) {
            // {"A":"...","B":"..."}
            node.fields().forEachRemaining(entry -> options.add(entry.getValue().asText()));
        } else if (node.isTextual()) {
            for (String line : node.asText().split("\\R")) {
                if (!line.isBlank()) {
                    options.add(line);
                }
            }
        }
        return options;
    }

    private static String answer(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isArray()) {
            List<String> parts = new ArrayList<>();
            node.forEach(part -> parts.add(part.asText().trim()));
            return String.join(",", parts);
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? "正确" : "错误";
        }
        return node.asText();
    }

    private static Integer score(JsonNode node, int defaultScore) {
        if (node != null && node.isNumber() && node.asInt() > 0) {
            return node.asInt();
        }
        if (node != null && node.isTextual()) {
            String digits = node.asText().replaceAll("[^0-9]", "");
            if (!digits.isEmpty() && digits.length() < 5 && Integer.parseInt(digits) > 0) {
                return Integer.parseInt(digits);
            }
        }
        return defaultScore > 0 ? defaultScore : null;
    }

    private static String abbreviate(String raw) {
        return raw.length() <= RAW_LIMIT ? raw : raw.substring(0, RAW_LIMIT) + "\n…（后面还有 " + (raw.length() - RAW_LIMIT) + " 个字符）";
    }
}
