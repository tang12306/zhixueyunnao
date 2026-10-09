package com._1.service.ai;

import com._1.dto.ai.BatchQuestionRequest;
import com._1.dto.ai.ExamGenerationRequest;
import com._1.entity.Chapter;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.repository.ChapterRepository;
import com._1.repository.SubjectRepository;
import com._1.service.ai.AiPromptBuilder.ExamPlanItem;
import com._1.service.ai.DeepSeekClient.ChatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * AI 出题：查学科和章节、拼提示词、调用 DeepSeek、解析结果。
 * <p>
 * 分两步：prepare 在请求线程里校验参数、查数据库、拼提示词（出错直接返回 400/503）；
 * run 只调用模型和解析，可以放到后台线程执行（见 {@link AiTaskService}）。
 */
@Service
public class AiQuestionService {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionService.class);
    static final String WAITING_STAGE = "等待 AI 回复";
    static final String PARSING_STAGE = "整理题目";

    private final DeepSeekClient deepSeekClient;
    private final AiPromptBuilder promptBuilder;
    private final AiQuestionParser parser;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;

    public AiQuestionService(DeepSeekClient deepSeekClient, AiPromptBuilder promptBuilder, AiQuestionParser parser,
                             SubjectRepository subjectRepository, ChapterRepository chapterRepository) {
        this.deepSeekClient = deepSeekClient;
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.subjectRepository = subjectRepository;
        this.chapterRepository = chapterRepository;
    }

    public boolean isConfigured() {
        return deepSeekClient.isConfigured();
    }

    /** 批量出题：参数已校验、提示词已拼好 */
    public record QuestionJob(QuestionType type, int count, String prompt) {
    }

    /** 一键出卷：参数已校验、提示词已拼好 */
    public record ExamJob(ExamGenerationRequest request, Map<QuestionType, ExamPlanItem> plan,
                          Long subjectId, String subjectName, String prompt) {
    }

    /** 同步出题，旧版题目编辑页使用 */
    public List<QuestionDraft> generateQuestions(BatchQuestionRequest request) {
        return runQuestions(prepareQuestions(request), stage -> { });
    }

    /** 同步出卷 */
    public ExamDraft generateExam(ExamGenerationRequest request) {
        return runExam(prepareExam(request), stage -> { });
    }

    public QuestionJob prepareQuestions(BatchQuestionRequest request) {
        QuestionType type = QuestionType.parse(request.getType())
                .orElseThrow(() -> new IllegalArgumentException("未知题型: " + request.getType()));
        Subject subject = findSubject(request.getSubjectId());
        List<Chapter> chapters = findChapters(subject, request.getChapterIds(), false);
        requireConfigured();

        String prompt = promptBuilder.questionsPrompt(subject.getName(), chapters, type,
                request.getDifficulty(), request.getCount(), request.getCustomPrompt());
        return new QuestionJob(type, request.getCount(), prompt);
    }

    /**
     * 按题型批量出题，返回的每道题 type 都等于请求的题型。
     */
    public List<QuestionDraft> runQuestions(QuestionJob job, Consumer<String> stage) {
        QuestionType type = job.type();
        stage.accept(WAITING_STAGE);
        ChatResult result = deepSeekClient.chat(AiPromptBuilder.SYSTEM_PROMPT, job.prompt(), true);
        stage.accept(PARSING_STAGE);

        // 批量出题只有一个题型，模型写错的 type 一律纠正
        List<QuestionDraft> questions = parser.parse(result.content(), type, 5, result.truncated()).stream()
                .map(q -> q.type() == type ? q : new QuestionDraft(type, q.content(),
                        type.isChoice() ? q.options() : List.of(),
                        QuestionTextNormalizer.normalizeAnswer(type, q.answer()), q.analysis(), q.score()))
                .toList();
        log.info("批量出题完成：题型 {}，请求 {} 道，生成 {} 道", type, job.count(), questions.size());
        return questions;
    }

    public ExamJob prepareExam(ExamGenerationRequest request) {
        Map<QuestionType, ExamPlanItem> plan = normalizePlan(request.getQuestionPlan());
        Subject subject = findSubject(request.getSubjectId());
        List<Chapter> chapters = findChapters(subject, request.getChapterIds(), true);
        requireConfigured();

        String prompt = promptBuilder.examPrompt(subject.getName(), request.getName(), request.getDuration(),
                request.getTargetScore(), chapters, plan, request.getChapterDistribution(),
                request.getExamRequirements());
        return new ExamJob(request, plan, subject.getId(), subject.getName(), prompt);
    }

    /**
     * 一键出卷：按计划一次生成整套试卷。分值以教师的计划为准，题目按题型排序。
     */
    public ExamDraft runExam(ExamJob job, Consumer<String> stage) {
        Map<QuestionType, ExamPlanItem> plan = job.plan();
        ExamGenerationRequest request = job.request();
        stage.accept(WAITING_STAGE);
        ChatResult result = deepSeekClient.chat(AiPromptBuilder.SYSTEM_PROMPT, job.prompt(), true);
        stage.accept(PARSING_STAGE);
        List<QuestionDraft> parsed = parser.parse(result.content(), null, 0, result.truncated());

        List<QuestionDraft> questions = new ArrayList<>();
        for (QuestionDraft question : parsed) {
            ExamPlanItem item = plan.get(question.type());
            questions.add(item != null ? question.withScore(item.scorePerQuestion()) : question);
        }
        questions.sort(Comparator.comparing(QuestionDraft::type));

        List<String> warnings = compareWithPlan(plan, questions, result.truncated());
        int totalScore = questions.stream().map(QuestionDraft::score).filter(Objects::nonNull)
                .mapToInt(Integer::intValue).sum();
        log.info("一键出卷完成：计划 {} 道，生成 {} 道，提示 {} 条",
                plan.values().stream().mapToInt(ExamPlanItem::count).sum(), questions.size(), warnings.size());
        return new ExamDraft(request.getName(), request.getDescription(), request.getDuration(),
                request.getTargetScore(), totalScore, job.subjectId(), job.subjectName(), questions, warnings);
    }

    /** 题型键接受枚举名和中文名，去掉数量为 0 的题型，按枚举顺序排列 */
    static Map<QuestionType, ExamPlanItem> normalizePlan(Map<String, ExamGenerationRequest.QuestionPlan> rawPlan) {
        Map<QuestionType, ExamPlanItem> plan = new EnumMap<>(QuestionType.class);
        if (rawPlan != null) {
            for (Map.Entry<String, ExamGenerationRequest.QuestionPlan> entry : rawPlan.entrySet()) {
                QuestionType type = QuestionType.parse(entry.getKey())
                        .orElseThrow(() -> new IllegalArgumentException("未知题型: " + entry.getKey()));
                ExamGenerationRequest.QuestionPlan item = entry.getValue();
                if (item != null && item.getCount() > 0) {
                    plan.merge(type, new ExamPlanItem(item.getCount(), item.getScorePerQuestion()),
                            (a, b) -> new ExamPlanItem(a.count() + b.count(), b.scorePerQuestion()));
                }
            }
        }
        if (plan.isEmpty()) {
            throw new IllegalArgumentException("请至少为一种题型设置题目数量");
        }
        int total = plan.values().stream().mapToInt(ExamPlanItem::count).sum();
        if (total > ExamGenerationRequest.MAX_TOTAL_QUESTIONS) {
            throw new IllegalArgumentException("一套试卷最多 " + ExamGenerationRequest.MAX_TOTAL_QUESTIONS
                    + " 道题，当前计划 " + total + " 道");
        }
        return plan;
    }

    private static List<String> compareWithPlan(Map<QuestionType, ExamPlanItem> plan, List<QuestionDraft> questions,
                                                boolean truncated) {
        Map<QuestionType, Integer> actual = new LinkedHashMap<>();
        questions.forEach(q -> actual.merge(q.type(), 1, Integer::sum));

        List<String> warnings = new ArrayList<>();
        if (truncated) {
            warnings.add("AI 回复超过长度上限被截断，部分题目可能缺失");
        }
        LinkedHashSet<QuestionType> types = new LinkedHashSet<>(plan.keySet());
        types.addAll(actual.keySet());
        for (QuestionType type : types) {
            int expected = plan.containsKey(type) ? plan.get(type).count() : 0;
            int got = actual.getOrDefault(type, 0);
            if (expected != got) {
                warnings.add(type.getLabel() + "计划 " + expected + " 道，实际生成 " + got + " 道");
            }
        }
        return warnings;
    }

    private void requireConfigured() {
        if (!deepSeekClient.isConfigured()) {
            throw AiServiceException.notConfigured();
        }
    }

    private Subject findSubject(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("学科不存在: " + subjectId));
    }

    /**
     * 一次查出所选章节，并检查都属于该学科。没选章节时：出卷用学科全部章节，批量出题不限章节。
     */
    private List<Chapter> findChapters(Subject subject, List<Long> chapterIds, boolean defaultToAll) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return defaultToAll ? chapterRepository.findBySubjectOrderByOrderNumAsc(subject) : List.of();
        }
        List<Long> ids = chapterIds.stream().filter(Objects::nonNull).distinct().toList();
        List<Chapter> chapters = new ArrayList<>(chapterRepository.findAllById(ids));
        if (chapters.size() != ids.size()) {
            throw new IllegalArgumentException("部分章节不存在，请刷新后重新选择");
        }
        for (Chapter chapter : chapters) {
            if (chapter.getSubject() == null || !Objects.equals(chapter.getSubject().getId(), subject.getId())) {
                throw new IllegalArgumentException("章节《" + chapter.getName() + "》不属于所选学科");
            }
        }
        chapters.sort(Comparator.comparing(Chapter::getOrderNum, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Chapter::getId));
        return chapters;
    }
}
