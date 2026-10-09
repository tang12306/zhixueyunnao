package com._1.controller;

import com._1.entity.Chapter;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.repository.ChapterRepository;
import com._1.repository.QuestionRepository;
import com._1.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 接口的参数校验、错误状态码和保存/导出。测试环境没有配置 DEEPSEEK_API_KEY，不会真正调用大模型。
 * 每个测试用不同的用户名，避免共用 AI 限流计数。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiControllerTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Subject subject;
    private Chapter chapter;

    @BeforeEach
    void createSubject() {
        subject = new Subject();
        subject.setName("AI测试学科-" + UUID.randomUUID());
        subject = subjectRepository.save(subject);

        chapter = new Chapter();
        chapter.setName("第一章");
        chapter.setOrderNum(1);
        chapter.setSubject(subject);
        chapter = chapterRepository.save(chapter);
    }

    @Test
    @WithMockUser(username = "ai.validation", roles = "TEACHER")
    void invalidExamRequestGets400WithReadableMessage() throws Exception {
        mvc.perform(post("/api/ai/generate-exam").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("请填写考试名称")))
                .andExpect(jsonPath("$.message", containsString("请选择学科")));

        mvc.perform(post("/api/ai/generate-exam").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(subject.getId(), "\"ESSAY\": {\"count\": 2, \"scorePerQuestion\": 5}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("未知题型")));
    }

    @Test
    @WithMockUser(username = "ai.unknown.subject", roles = "TEACHER")
    void unknownSubjectGets400() throws Exception {
        mvc.perform(post("/api/ai/generate-exam").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(990000058L, "\"SINGLE_CHOICE\": {\"count\": 2, \"scorePerQuestion\": 5}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("学科不存在")));
    }

    @Test
    @WithMockUser(username = "ai.not.configured", roles = "TEACHER")
    void missingApiKeyGets503() throws Exception {
        mvc.perform(post("/api/ai/generate-exam").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examJson(subject.getId(), "\"填空题\": {\"count\": 2, \"scorePerQuestion\": 5}")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("DEEPSEEK_API_KEY")));

        mvc.perform(post("/api/generate-question").with(csrf())
                        .param("subjectId", String.valueOf(subject.getId()))
                        .param("type", "FILL_BLANK"))
                .andExpect(status().isServiceUnavailable());

        mvc.perform(get("/api/ai/health-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    @WithMockUser(username = "ai.save", roles = "TEACHER")
    void savesQuestionsWithLegacyTypeNames() throws Exception {
        long before = questionRepository.count();
        String body = """
                [{"subjectId": %d, "chapterId": %d, "type": "FILL_BLANK", "content": "HTTP 默认端口是 ____。", "answer": "80"},
                 {"subjectId": %d, "type": "多选题", "content": "传输层协议有？",
                  "options": ["A. TCP", "B. UDP", "C. IP"], "answer": ["B", "A"], "score": 4}]"""
                .formatted(subject.getId(), chapter.getId(), subject.getId());

        mvc.perform(post("/api/ai/save-questions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(2));

        assertThat(questionRepository.count()).isEqualTo(before + 2);
        // 选项是懒加载的集合，要在事务里读
        new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
            List<Question> saved = questionRepository.findAll().stream()
                    .filter(q -> subject.getName().equals(q.getSubject())).toList();
            assertThat(saved).extracting(Question::getType)
                    .containsExactlyInAnyOrder(QuestionType.FILL_IN_THE_BLANK, QuestionType.MULTIPLE_CHOICE);
            Question multiple = saved.stream()
                    .filter(q -> q.getType() == QuestionType.MULTIPLE_CHOICE).findFirst().orElseThrow();
            assertThat(multiple.getAnswer()).isEqualTo("A,B");
            assertThat(multiple.getOptions()).containsExactly("TCP", "UDP", "IP");
            assertThat(multiple.getScore()).isEqualTo(4);
        });
    }

    @Test
    @WithMockUser(username = "ai.save.invalid", roles = "TEACHER")
    void invalidQuestionInBatchSavesNothing() throws Exception {
        long before = questionRepository.count();
        String body = """
                [{"subjectId": %d, "type": "SINGLE_CHOICE", "content": "有效题目", "options": ["a", "b"], "answer": "A"},
                 {"subjectId": %d, "type": "作文题", "content": "无效题型"}]"""
                .formatted(subject.getId(), subject.getId());

        mvc.perform(post("/api/ai/save-questions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("第 2 题")));

        assertThat(questionRepository.count()).isEqualTo(before);
    }

    @Test
    @WithMockUser(username = "ai.export", roles = "TEACHER")
    void exportsWordWithUtf8FileName() throws Exception {
        String body = """
                {"name": "期中考试/A卷", "questions": [
                  {"type": "SINGLE_CHOICE", "content": "TCP 属于哪一层？", "options": ["应用层", "传输层"], "answer": "B", "score": 2}
                ]}""";

        byte[] docx = mvc.perform(post("/api/ai/export/word").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("filename*=UTF-8''")))
                .andReturn().getResponse().getContentAsByteArray();

        // docx 是 zip，以 PK 开头
        assertThat(new String(docx, 0, 2, StandardCharsets.ISO_8859_1)).isEqualTo("PK");

        mvc.perform(post("/api/ai/export/word").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"空试卷\", \"questions\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("试卷里没有题目"));
    }

    private static String examJson(Long subjectId, String plan) {
        return """
                {"name": "期中考试", "duration": 90, "targetScore": 100, "subjectId": %d,
                 "questionPlan": {%s}}""".formatted(subjectId, plan);
    }
}
