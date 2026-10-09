package com._1.controller;

import com._1.entity.Chapter;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.repository.ChapterRepository;
import com._1.repository.QuestionRepository;
import com._1.repository.SubjectRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "TEACHER")
class QuestionControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    void queryBySubjectIncludesQuestionsWithoutChapterAndAcceptsChineseType() throws Exception {
        Subject network = subject("计算机网络-" + UUID.randomUUID());
        Subject other = subject("线性代数-" + UUID.randomUUID());
        Chapter chapter = chapter(network, "传输层");

        question(network.getName(), chapter, QuestionType.FILL_IN_THE_BLANK, "有章节的填空题");
        // AI 按多个章节出题后保存，只记学科名称
        question(network.getName(), null, QuestionType.FILL_IN_THE_BLANK, "没有章节的填空题");
        question(network.getName(), chapter, QuestionType.SINGLE_CHOICE, "有章节的单选题");
        question(other.getName(), null, QuestionType.FILL_IN_THE_BLANK, "其他学科的填空题");

        mvc.perform(get("/questions/api/query")
                        .param("subjectId", String.valueOf(network.getId()))
                        .param("type", "填空题"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].content", containsInAnyOrder("有章节的填空题", "没有章节的填空题")));

        mvc.perform(get("/questions/api/query").param("subjectId", String.valueOf(network.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void questionFormOffersEnumTypes() throws Exception {
        mvc.perform(get("/questions/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"FILL_IN_THE_BLANK\"")))
                .andExpect(content().string(containsString("value=\"TRUE_FALSE\"")))
                .andExpect(content().string(not(containsString("value=\"FILL_BLANK\""))))
                .andExpect(content().string(not(containsString("value=\"CHOICE\""))));
    }

    @Test
    void createsAndUpdatesQuestionFromJson() throws Exception {
        Subject network = subject("计算机网络-" + UUID.randomUUID());
        Chapter chapter = chapter(network, "传输层");

        String created = mvc.perform(post("/questions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": %d, "chapterId": %d, "type": "MULTIPLE_CHOICE", "difficulty": 4,
                                 "content": "传输层协议有？", "options": ["TCP", "UDP", "IP"], "answer": "B,A",
                                 "tags": ["协议"]}""".formatted(network.getId(), chapter.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/questions/")))
                .andExpect(jsonPath("$.subject").value(network.getName()))
                .andExpect(jsonPath("$.chapter.id").value(chapter.getId()))
                .andExpect(jsonPath("$.answer").value("A,B"))
                .andExpect(jsonPath("$.tags[0]").value("协议"))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.id");

        mvc.perform(put("/questions/{id}", id.longValue()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": %d, "type": "TRUE_FALSE", "difficulty": 2,
                                 "content": "UDP 是面向连接的。", "options": ["多余"], "answer": "错"}"""
                                .formatted(network.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.longValue()))
                .andExpect(jsonPath("$.type").value("TRUE_FALSE"))
                .andExpect(jsonPath("$.answer").value("错误"))
                .andExpect(jsonPath("$.options").isEmpty())
                .andExpect(jsonPath("$.chapter").value(nullValue()));
    }

    @Test
    void rejectsInvalidQuestionJson() throws Exception {
        Subject network = subject("计算机网络-" + UUID.randomUUID());
        Chapter otherChapter = chapter(subject("线性代数-" + UUID.randomUUID()), "矩阵");
        long before = questionRepository.count();

        assertBadRequest("""
                {"subjectId": %d, "chapterId": %d, "type": "SHORT_ANSWER", "content": "简述三次握手。", "answer": "略"}"""
                .formatted(network.getId(), otherChapter.getId()), "不属于学科");
        assertBadRequest("""
                {"subjectId": %d, "type": "SINGLE_CHOICE", "content": "TCP 属于哪一层？", "options": ["传输层"], "answer": "A"}"""
                .formatted(network.getId()), "至少需要两个选项");
        assertBadRequest("""
                {"subjectId": %d, "type": "SHORT_ANSWER", "content": "简述三次握手。", "answer": " "}"""
                .formatted(network.getId()), "答案不能为空");
        assertBadRequest("""
                {"type": "SHORT_ANSWER", "content": "简述三次握手。", "answer": "略"}""", "请选择学科");

        assertThat(questionRepository.count()).isEqualTo(before);

        mvc.perform(put("/questions/{id}", Long.MAX_VALUE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subjectId": %d, "type": "SHORT_ANSWER", "content": "简述三次握手。", "answer": "略"}"""
                                .formatted(network.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void formPostStillGoesToLegacyPage() throws Exception {
        mvc.perform(post("/questions").with(csrf()).param("content", "旧版表单"))
                .andExpect(status().is3xxRedirection());
    }

    private void assertBadRequest(String body, String message) throws Exception {
        mvc.perform(post("/questions").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(message)));
    }

    private Subject subject(String name) {
        Subject subject = new Subject();
        subject.setName(name);
        return subjectRepository.save(subject);
    }

    private Chapter chapter(Subject subject, String name) {
        Chapter chapter = new Chapter();
        chapter.setName(name);
        chapter.setOrderNum(1);
        chapter.setSubject(subject);
        return chapterRepository.save(chapter);
    }

    private void question(String subject, Chapter chapter, QuestionType type, String content) {
        Question question = new Question();
        question.setSubject(subject);
        question.setChapter(chapter);
        question.setType(type);
        question.setDifficulty(3);
        question.setContent(content);
        question.setAnswer("略");
        questionRepository.save(question);
    }
}
