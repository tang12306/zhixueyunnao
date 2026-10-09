package com._1.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionTypeTests {

    @Test
    void parsesEnumNamesLabelsAndLegacyAliases() {
        assertThat(QuestionType.parse("SINGLE_CHOICE")).contains(QuestionType.SINGLE_CHOICE);
        assertThat(QuestionType.parse(" multiple-choice ")).contains(QuestionType.MULTIPLE_CHOICE);
        assertThat(QuestionType.parse("判断题")).contains(QuestionType.TRUE_FALSE);
        assertThat(QuestionType.parse("填空题")).contains(QuestionType.FILL_IN_THE_BLANK);
        assertThat(QuestionType.parse("简答题")).contains(QuestionType.SHORT_ANSWER);

        // 旧的一键出卷代码和旧模板用的写法
        assertThat(QuestionType.parse("FILL_BLANK")).contains(QuestionType.FILL_IN_THE_BLANK);
        assertThat(QuestionType.parse("CHOICE")).contains(QuestionType.SINGLE_CHOICE);
        assertThat(QuestionType.parse("选择题")).contains(QuestionType.SINGLE_CHOICE);
        assertThat(QuestionType.parse("问答题")).contains(QuestionType.SHORT_ANSWER);
    }

    @Test
    void unknownOrBlankTypesAreEmpty() {
        assertThat(QuestionType.parse(null)).isEmpty();
        assertThat(QuestionType.parse("  ")).isEmpty();
        assertThat(QuestionType.parse("ESSAY")).isEmpty();
    }

    @Test
    void jsonAcceptsLabelsButWritesEnumNames() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.readValue("\"填空题\"", QuestionType.class)).isEqualTo(QuestionType.FILL_IN_THE_BLANK);
        assertThat(mapper.readValue("\"FILL_BLANK\"", QuestionType.class)).isEqualTo(QuestionType.FILL_IN_THE_BLANK);
        assertThat(mapper.readValue("\"\"", QuestionType.class)).isNull();
        assertThat(mapper.writeValueAsString(QuestionType.TRUE_FALSE)).isEqualTo("\"TRUE_FALSE\"");
        assertThatThrownBy(() -> mapper.readValue("\"ESSAY\"", QuestionType.class))
                .isInstanceOf(JsonMappingException.class);
    }
}
