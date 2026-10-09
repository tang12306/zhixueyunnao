package com._1.service.ai;

import com._1.entity.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTextNormalizerTests {

    @Test
    void stripsSequentialLetterPrefixes() {
        assertThat(QuestionTextNormalizer.stripLetterPrefixes(List.of("A. 应用层", "B、传输层", "（C）网络层", "d: 链路层")))
                .containsExactly("应用层", "传输层", "网络层", "链路层");
    }

    @Test
    void keepsOptionsThatOnlyLookLikePrefixes() {
        // 没有分隔符，是正文
        assertThat(QuestionTextNormalizer.stripLetterPrefixes(List.of("A型血", "B型血", "AB型血", "O型血")))
                .containsExactly("A型血", "B型血", "AB型血", "O型血");
        // 字母顺序不对，不是选项编号
        assertThat(QuestionTextNormalizer.stripLetterPrefixes(List.of("B. 甲", "A. 乙")))
                .containsExactly("B. 甲", "A. 乙");
        assertThat(QuestionTextNormalizer.stripLetterPrefixes(null)).isEmpty();
    }

    @Test
    void normalizesChoiceAnswersToSortedLetters() {
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.SINGLE_CHOICE, " b ")).isEqualTo("B");
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.MULTIPLE_CHOICE, "[\"C\", \"A\"]")).isEqualTo("A,C");
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.MULTIPLE_CHOICE, "CA")).isEqualTo("A,C");
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.MULTIPLE_CHOICE, "a、c；d")).isEqualTo("A,C,D");
        // 带文字说明的答案不猜测，原样保留
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.SINGLE_CHOICE, "B. 传输层")).isEqualTo("B. 传输层");
    }

    @Test
    void normalizesTrueFalseAnswers() {
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.TRUE_FALSE, "对")).isEqualTo("正确");
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.TRUE_FALSE, "True")).isEqualTo("正确");
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.TRUE_FALSE, "×")).isEqualTo("错误");
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.TRUE_FALSE, "错误。")).isEqualTo("错误");
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.TRUE_FALSE, "视情况而定")).isEqualTo("视情况而定");
    }

    @Test
    void otherTypesAreOnlyTrimmed() {
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.FILL_IN_THE_BLANK, " 80;443 ")).isEqualTo("80;443");
        assertThat(QuestionTextNormalizer.normalizeAnswer(QuestionType.SHORT_ANSWER, null)).isEmpty();
    }
}
