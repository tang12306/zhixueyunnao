package com._1.service.ai;

import com._1.entity.QuestionType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static com._1.entity.QuestionType.FILL_IN_THE_BLANK;
import static com._1.entity.QuestionType.MULTIPLE_CHOICE;
import static com._1.entity.QuestionType.SHORT_ANSWER;
import static com._1.entity.QuestionType.SINGLE_CHOICE;
import static com._1.entity.QuestionType.TRUE_FALSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * 用模型实际出现过的几种回复格式测试解析器。
 */
class AiQuestionParserTests {

    private final AiQuestionParser parser = new AiQuestionParser();

    @Test
    void parsesAllFiveQuestionTypesFromFencedJson() {
        String raw = """
                好的，以下是试卷：
                ```json
                {
                  "questions": [
                    {"type": "SINGLE_CHOICE", "content": "TCP 属于哪一层？",
                     "options": ["A. 应用层", "B. 传输层", "C. 网络层", "D. 链路层"],
                     "answer": "B", "analysis": "TCP 是传输层协议", "score": 2},
                    {"type": "多选题", "content": "以下哪些是传输层协议？",
                     "options": [{"key": "A", "content": "TCP"}, {"key": "B", "content": "UDP"},
                                 {"key": "C", "content": "IP"}, {"key": "D", "content": "ARP"}],
                     "answer": ["B", "A"], "score": "4分"},
                    {"type": "TRUE_FALSE", "content": "UDP 提供可靠传输。", "options": [], "answer": false},
                    // 旧提示词里的题型写法
                    {"type": "FILL_BLANK", "content": "HTTP 默认端口是 ____。", "answer": "80"},
                    {"type": "SHORT_ANSWER", "content": "简述三次握手的过程。", "answer": "客户端发送 SYN……"},
                  ]
                }
                ```
                希望对你有帮助。""";

        List<QuestionDraft> questions = parser.parse(raw, null, 0, false);

        assertThat(questions).extracting(QuestionDraft::type)
                .containsExactly(SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, FILL_IN_THE_BLANK, SHORT_ANSWER);

        QuestionDraft single = questions.get(0);
        assertThat(single.options()).containsExactly("应用层", "传输层", "网络层", "链路层");
        assertThat(single.answer()).isEqualTo("B");
        assertThat(single.analysis()).isEqualTo("TCP 是传输层协议");
        assertThat(single.score()).isEqualTo(2);

        QuestionDraft multiple = questions.get(1);
        assertThat(multiple.options()).containsExactly("TCP", "UDP", "IP", "ARP");
        assertThat(multiple.answer()).isEqualTo("A,B");
        assertThat(multiple.score()).isEqualTo(4);

        assertThat(questions.get(2).answer()).isEqualTo("错误");
        assertThat(questions.get(2).options()).isEmpty();
        assertThat(questions.get(3).answer()).isEqualTo("80");
        assertThat(questions.get(4).analysis()).isEmpty();
        // 没写分值、也没有默认分值
        assertThat(questions.get(4).score()).isNull();
    }

    @Test
    void bareArrayUsesDefaultTypeAndScore() {
        String raw = """
                [{"question": "地球是圆的。", "answer": "√", "explanation": "常识"},
                 {"content": "水在 0 ℃ 一定结冰。", "answer": "否"}]""";

        List<QuestionDraft> questions = parser.parse(raw, TRUE_FALSE, 5, false);

        assertThat(questions).hasSize(2).allSatisfy(q -> {
            assertThat(q.type()).isEqualTo(TRUE_FALSE);
            assertThat(q.score()).isEqualTo(5);
        });
        assertThat(questions.get(0).content()).isEqualTo("地球是圆的。");
        assertThat(questions.get(0).answer()).isEqualTo("正确");
        assertThat(questions.get(0).analysis()).isEqualTo("常识");
        assertThat(questions.get(1).answer()).isEqualTo("错误");
    }

    @Test
    void infersTypeFromOptionsWhenMissing() {
        String raw = """
                {"items": [
                  {"content": "1+1=?", "options": {"A": "1", "B": "2"}, "answer": "b"},
                  {"content": "解释什么是导数。", "options": [], "answer": "变化率"}
                ]}""";

        List<QuestionDraft> questions = parser.parse(raw, null, 0, false);

        assertThat(questions).extracting(QuestionDraft::type).containsExactly(SINGLE_CHOICE, SHORT_ANSWER);
        assertThat(questions.get(0).options()).containsExactly("1", "2");
        assertThat(questions.get(0).answer()).isEqualTo("B");
    }

    @Test
    void nonChoiceQuestionsDropOptions() {
        String raw = """
                {"questions": [{"type": "填空题", "content": "____ 是传输层协议。",
                                "options": ["A. TCP", "B. IP"], "answer": "TCP"}]}""";

        QuestionDraft question = parser.parse(raw, null, 0, false).get(0);

        assertThat(question.type()).isEqualTo(FILL_IN_THE_BLANK);
        assertThat(question.options()).isEmpty();
    }

    @Test
    void skipsQuestionsWithoutContent() {
        String raw = """
                {"questions": [{"type": "SINGLE_CHOICE", "content": ""}, {"type": "SHORT_ANSWER", "content": "有效题目"}]}""";

        assertThat(parser.parse(raw, null, 0, false)).extracting(QuestionDraft::content).containsExactly("有效题目");
    }

    @Test
    void truncatedReplyFailsWithRawText() {
        String raw = """
                {"questions": [{"type": "SINGLE_CHOICE", "content": "题一", "options": ["a", "b"], "answer": "A"},
                {"type": "SINGLE_CHOICE", "content": "题""";

        AiServiceException e = catchThrowableOfType(() -> parser.parse(raw, null, 0, true), AiServiceException.class);

        assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(e.getMessage()).contains("截断");
        assertThat(e.getRaw()).isEqualTo(raw);
    }

    @Test
    void plainTextReplyFailsWithRawText() {
        String raw = "抱歉，我无法生成这些题目。";

        AiServiceException e = catchThrowableOfType(() -> parser.parse(raw, null, 0, false), AiServiceException.class);

        assertThat(e.getMessage()).contains("不是有效的 JSON");
        assertThat(e.getRaw()).isEqualTo(raw);
    }

    @Test
    void jsonWithoutQuestionsFails() {
        AiServiceException noList = catchThrowableOfType(
                () -> parser.parse("{\"error\": \"busy\"}", null, 0, false), AiServiceException.class);
        assertThat(noList.getMessage()).contains("没有题目列表");

        AiServiceException empty = catchThrowableOfType(
                () -> parser.parse("{\"questions\": []}", null, 0, false), AiServiceException.class);
        assertThat(empty.getMessage()).contains("没有可用的题目");

        AiServiceException blank = catchThrowableOfType(
                () -> parser.parse("  ", QuestionType.SHORT_ANSWER, 0, false), AiServiceException.class);
        assertThat(blank.getMessage()).contains("没有返回任何内容");
    }

    @Test
    void longRawTextIsAbbreviated() {
        String raw = "不是 JSON " + "很长的回复".repeat(2000);

        AiServiceException e = catchThrowableOfType(() -> parser.parse(raw, null, 0, false), AiServiceException.class);

        assertThat(e.getRaw()).startsWith(raw.substring(0, AiQuestionParser.RAW_LIMIT))
                .contains("后面还有 " + (raw.length() - AiQuestionParser.RAW_LIMIT) + " 个字符");
        assertThat(e.getData()).isEqualTo(java.util.Map.of("raw", e.getRaw()));
    }
}
