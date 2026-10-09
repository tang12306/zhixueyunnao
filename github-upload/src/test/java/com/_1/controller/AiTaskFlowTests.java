package com._1.controller;

import com._1.entity.Subject;
import com._1.repository.SubjectRepository;
import com._1.service.ai.DeepSeekClient;
import com._1.service.ai.DeepSeekClient.ChatResult;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 异步出题的完整流程：提交得到 202 和任务 id，轮询拿到结果；别的教师查不到这个任务。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiTaskFlowTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @MockBean
    private DeepSeekClient deepSeekClient;

    private Subject subject;

    @BeforeEach
    void setUp() {
        subject = new Subject();
        subject.setName("异步出题学科-" + UUID.randomUUID());
        subject = subjectRepository.save(subject);
        when(deepSeekClient.isConfigured()).thenReturn(true);
    }

    @Test
    void batchQuestionsRunInBackgroundAndArePolled() throws Exception {
        when(deepSeekClient.chat(anyString(), anyString(), anyBoolean())).thenReturn(new ChatResult("""
                {"questions": [{"content": "TCP 是面向连接的协议。", "answer": "对", "analysis": "三次握手"}]}""",
                false));

        String body = """
                {"subjectId": %d, "type": "TRUE_FALSE", "count": 1}""".formatted(subject.getId());
        String submitted = mvc.perform(post("/api/ai/generate-batch-questions").with(csrf())
                        .with(user("ai.flow").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String taskId = JsonPath.read(submitted, "$.taskId");

        String finished = pollUntilFinished(taskId, "ai.flow");
        assertThat((String) JsonPath.read(finished, "$.status")).isEqualTo("SUCCEEDED");
        assertThat((String) JsonPath.read(finished, "$.result[0].type")).isEqualTo("TRUE_FALSE");
        assertThat((String) JsonPath.read(finished, "$.result[0].answer")).isEqualTo("正确");

        mvc.perform(get("/api/ai/tasks/" + taskId).with(user("ai.flow.other").roles("TEACHER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void unparseableReplyFailsWithRawText() throws Exception {
        when(deepSeekClient.chat(anyString(), anyString(), anyBoolean()))
                .thenReturn(new ChatResult("抱歉，我无法生成这份试卷。", false));

        String body = """
                {"name": "期中考试", "duration": 90, "targetScore": 100, "subjectId": %d,
                 "questionPlan": {"SINGLE_CHOICE": {"count": 2, "scorePerQuestion": 5}}}""".formatted(subject.getId());
        String submitted = mvc.perform(post("/api/ai/generate-exam").with(csrf())
                        .with(user("ai.flow.fail").roles("TEACHER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String finished = pollUntilFinished(JsonPath.read(submitted, "$.taskId"), "ai.flow.fail");
        assertThat((String) JsonPath.read(finished, "$.status")).isEqualTo("FAILED");
        assertThat((Integer) JsonPath.read(finished, "$.error.status")).isEqualTo(502);
        assertThat((String) JsonPath.read(finished, "$.error.raw")).contains("无法生成");
    }

    private String pollUntilFinished(String taskId, String username) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            String json = mvc.perform(get("/api/ai/tasks/" + taskId).with(user(username).roles("TEACHER")))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            String taskStatus = JsonPath.read(json, "$.status");
            if (taskStatus.equals("SUCCEEDED") || taskStatus.equals("FAILED")) {
                return json;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("任务 5 秒内没有结束");
    }
}
