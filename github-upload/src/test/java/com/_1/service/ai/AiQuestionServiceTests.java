package com._1.service.ai;

import com._1.dto.ai.BatchQuestionRequest;
import com._1.dto.ai.ExamGenerationRequest;
import com._1.dto.ai.ExamGenerationRequest.QuestionPlan;
import com._1.entity.Chapter;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.repository.ChapterRepository;
import com._1.repository.SubjectRepository;
import com._1.service.ai.AiPromptBuilder.ExamPlanItem;
import com._1.service.ai.DeepSeekClient.ChatResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiQuestionServiceTests {

    private final DeepSeekClient deepSeekClient = mock(DeepSeekClient.class);
    private final SubjectRepository subjectRepository = mock(SubjectRepository.class);
    private final ChapterRepository chapterRepository = mock(ChapterRepository.class);
    private final AiQuestionService service = new AiQuestionService(deepSeekClient, new AiPromptBuilder(),
            new AiQuestionParser(), subjectRepository, chapterRepository);

    private final Subject network = subject(1L, "计算机网络");

    @BeforeEach
    void setUp() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(network));
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());
        when(deepSeekClient.isConfigured()).thenReturn(true);
    }

    // ---- 准备阶段 ----

    @Test
    void prepareFailsFastWhenKeyIsMissing() {
        when(deepSeekClient.isConfigured()).thenReturn(false);
        when(chapterRepository.findBySubjectOrderByOrderNumAsc(network)).thenReturn(List.of());

        assertThatThrownBy(() -> service.prepareExam(examRequest(Map.of("SINGLE_CHOICE", plan(2, 3)))))
                .isInstanceOfSatisfying(AiServiceException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE))
                .hasMessageContaining("DEEPSEEK_API_KEY");
        verify(deepSeekClient, never()).chat(anyString(), anyString(), anyBoolean());
    }

    @Test
    void parameterErrorsWinOverMissingKey() {
        // 参数错了先告诉教师参数错，而不是先报 AI 未配置
        when(deepSeekClient.isConfigured()).thenReturn(false);
        ExamGenerationRequest request = examRequest(Map.of("SINGLE_CHOICE", plan(2, 3)));
        request.setSubjectId(99L);

        assertThatThrownBy(() -> service.prepareExam(request))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("学科不存在");
    }

    @Test
    void runReportsStagesInOrder() {
        when(chapterRepository.findBySubjectOrderByOrderNumAsc(network)).thenReturn(List.of());
        reply("""
                {"questions": [{"type": "SINGLE_CHOICE", "content": "题一", "options": ["a", "b"], "answer": "A"}]}""",
                false);
        List<String> stages = new ArrayList<>();

        service.runExam(service.prepareExam(examRequest(Map.of("SINGLE_CHOICE", plan(1, 3)))), stages::add);

        assertThat(stages).containsExactly(AiQuestionService.WAITING_STAGE, AiQuestionService.PARSING_STAGE);
    }

    // ---- 题型计划 ----

    @Test
    void planAcceptsLabelsAndAliasesAndDropsZeroCounts() {
        Map<String, QuestionPlan> raw = new LinkedHashMap<>();
        raw.put("简答题", plan(2, 10));
        raw.put("FILL_BLANK", plan(3, 2));
        raw.put("填空题", plan(2, 4));
        raw.put("MULTIPLE_CHOICE", plan(0, 4));
        raw.put("判断题", plan(5, 2));

        Map<QuestionType, ExamPlanItem> normalized = AiQuestionService.normalizePlan(raw);

        // 按枚举顺序；同一题型的两种写法合并
        assertThat(normalized).containsExactly(
                Map.entry(QuestionType.TRUE_FALSE, new ExamPlanItem(5, 2)),
                Map.entry(QuestionType.FILL_IN_THE_BLANK, new ExamPlanItem(5, 4)),
                Map.entry(QuestionType.SHORT_ANSWER, new ExamPlanItem(2, 10)));
    }

    @Test
    void planRejectsUnknownEmptyAndOversizedPlans() {
        assertThatThrownBy(() -> AiQuestionService.normalizePlan(Map.of("ESSAY", plan(1, 10))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("未知题型");
        assertThatThrownBy(() -> AiQuestionService.normalizePlan(Map.of("SINGLE_CHOICE", plan(0, 2))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("至少");
        assertThatThrownBy(() -> AiQuestionService.normalizePlan(Map.of(
                "SINGLE_CHOICE", plan(40, 1), "TRUE_FALSE", plan(21, 1))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("最多 60 道");
    }

    // ---- 一键出卷 ----

    @Test
    void examUsesPlannedScoresAndSortsByType() {
        when(chapterRepository.findBySubjectOrderByOrderNumAsc(network)).thenReturn(List.of());
        reply("""
                {"questions": [
                  {"type": "TRUE_FALSE", "content": "UDP 可靠。", "answer": "错", "score": 10},
                  {"type": "SINGLE_CHOICE", "content": "题一", "options": ["a", "b"], "answer": "A", "score": 10},
                  {"type": "SINGLE_CHOICE", "content": "题二", "options": ["a", "b"], "answer": "b"}
                ]}""", false);

        ExamDraft exam = service.generateExam(examRequest(Map.of("单选题", plan(2, 3), "判断题", plan(1, 2))));

        assertThat(exam.questions()).extracting(QuestionDraft::type)
                .containsExactly(QuestionType.SINGLE_CHOICE, QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE);
        assertThat(exam.questions()).extracting(QuestionDraft::score).containsExactly(3, 3, 2);
        assertThat(exam.totalScore()).isEqualTo(8);
        assertThat(exam.subjectName()).isEqualTo("计算机网络");
        assertThat(exam.warnings()).isEmpty();
        verify(deepSeekClient).chat(eq(AiPromptBuilder.SYSTEM_PROMPT), contains("判断题（TRUE_FALSE）：1 道"), eq(true));
    }

    @Test
    void examWarnsWhenModelDeviatesFromPlan() {
        when(chapterRepository.findBySubjectOrderByOrderNumAsc(network)).thenReturn(List.of());
        reply("""
                {"questions": [
                  {"type": "SINGLE_CHOICE", "content": "题一", "options": ["a", "b"], "answer": "A"},
                  {"type": "SHORT_ANSWER", "content": "多出来的简答题", "answer": "略"}
                ]}""", true);

        ExamDraft exam = service.generateExam(examRequest(Map.of("SINGLE_CHOICE", plan(2, 3))));

        assertThat(exam.warnings()).containsExactly(
                "AI 回复超过长度上限被截断，部分题目可能缺失",
                "单选题计划 2 道，实际生成 1 道",
                "简答题计划 0 道，实际生成 1 道");
    }

    @Test
    void examRejectsChaptersFromAnotherSubject() {
        Chapter foreign = chapter(7L, "线性方程组", subject(2L, "线性代数"));
        when(chapterRepository.findAllById(List.of(7L))).thenReturn(List.of(foreign));

        ExamGenerationRequest request = examRequest(Map.of("SINGLE_CHOICE", plan(2, 3)));
        request.setChapterIds(List.of(7L));

        assertThatThrownBy(() -> service.generateExam(request))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("不属于所选学科");
        verify(deepSeekClient, never()).chat(anyString(), anyString(), anyBoolean());
    }

    @Test
    void examRejectsMissingChaptersAndSubjects() {
        when(chapterRepository.findAllById(List.of(7L, 8L))).thenReturn(List.of(chapter(7L, "第一章", network)));

        ExamGenerationRequest missingChapter = examRequest(Map.of("SINGLE_CHOICE", plan(2, 3)));
        missingChapter.setChapterIds(List.of(7L, 8L, 8L));
        assertThatThrownBy(() -> service.generateExam(missingChapter)).hasMessageContaining("部分章节不存在");

        ExamGenerationRequest missingSubject = examRequest(Map.of("SINGLE_CHOICE", plan(2, 3)));
        missingSubject.setSubjectId(99L);
        assertThatThrownBy(() -> service.generateExam(missingSubject)).hasMessageContaining("学科不存在");
    }

    // ---- 批量出题 ----

    @Test
    void batchForcesRequestedTypeAndDefaultScore() {
        Chapter first = chapter(7L, "第一章", network);
        when(chapterRepository.findAllById(List.of(7L))).thenReturn(List.of(first));
        reply("""
                {"questions": [
                  {"type": "SINGLE_CHOICE", "content": "HTTP 默认端口是 ____。", "options": ["80", "443"], "answer": "80"}
                ]}""", false);

        BatchQuestionRequest request = new BatchQuestionRequest();
        request.setSubjectId(1L);
        request.setChapterIds(List.of(7L));
        request.setType("填空题");
        request.setCount(1);

        List<QuestionDraft> questions = service.generateQuestions(request);

        assertThat(questions).singleElement().satisfies(q -> {
            assertThat(q.type()).isEqualTo(QuestionType.FILL_IN_THE_BLANK);
            assertThat(q.options()).isEmpty();
            assertThat(q.answer()).isEqualTo("80");
            assertThat(q.score()).isEqualTo(5);
        });
        verify(deepSeekClient).chat(anyString(), contains("- 第一章"), eq(true));
    }

    @ParameterizedTest
    @EnumSource(QuestionType.class)
    void batchGeneratesEveryQuestionType(QuestionType type) {
        // 模型没写 type 时以请求的题型为准；选择题保留选项，其他题型去掉选项
        reply("""
                {"questions": [
                  {"content": "第一题", "options": ["甲", "乙", "丙"], "answer": "A", "analysis": "解析一"},
                  {"content": "第二题", "options": ["甲", "乙", "丙"], "answer": "B", "score": 8}
                ]}""", false);

        BatchQuestionRequest request = new BatchQuestionRequest();
        request.setSubjectId(1L);
        request.setType(type.name());
        request.setCount(2);

        List<QuestionDraft> questions = service.generateQuestions(request);

        assertThat(questions).hasSize(2).allSatisfy(q -> {
            assertThat(q.type()).isEqualTo(type);
            assertThat(q.content()).isNotBlank();
            assertThat(q.answer()).isNotBlank();
            assertThat(q.options()).hasSize(type.isChoice() ? 3 : 0);
        });
        assertThat(questions).extracting(QuestionDraft::score).containsExactly(5, 8);
        verify(deepSeekClient).chat(anyString(), contains(type.getLabel()), eq(true));
    }

    private void reply(String content, boolean truncated) {
        when(deepSeekClient.chat(anyString(), anyString(), anyBoolean())).thenReturn(new ChatResult(content, truncated));
    }

    private static ExamGenerationRequest examRequest(Map<String, QuestionPlan> plan) {
        ExamGenerationRequest request = new ExamGenerationRequest();
        request.setName("期中考试");
        request.setDuration(90);
        request.setTargetScore(100);
        request.setSubjectId(1L);
        request.setQuestionPlan(new LinkedHashMap<>(plan));
        return request;
    }

    private static QuestionPlan plan(int count, int score) {
        QuestionPlan plan = new QuestionPlan();
        plan.setCount(count);
        plan.setScorePerQuestion(score);
        return plan;
    }

    private static Subject subject(Long id, String name) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setName(name);
        return subject;
    }

    private static Chapter chapter(Long id, String name, Subject subject) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setName(name);
        chapter.setSubject(subject);
        chapter.setOrderNum(1);
        return chapter;
    }
}
