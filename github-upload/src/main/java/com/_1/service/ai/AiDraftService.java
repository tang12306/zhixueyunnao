package com._1.service.ai;

import com._1.core.exception.ApiException;
import com._1.dto.ai.SaveQuestionRequest;
import com._1.entity.Chapter;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.repository.ChapterRepository;
import com._1.repository.QuestionRepository;
import com._1.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 把 AI 生成、经教师编辑过的题目存进题库；题库页手动录入、修改题目也走这里，校验规则一致。
 */
@Service
public class AiDraftService {

    public static final int MAX_BATCH = 200;
    private static final int DEFAULT_DIFFICULTY = 3;

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;

    public AiDraftService(QuestionRepository questionRepository, SubjectRepository subjectRepository,
                          ChapterRepository chapterRepository) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.chapterRepository = chapterRepository;
    }

    /**
     * 全部校验通过才保存，任何一题有问题都不入库，错误信息里带题号。
     *
     * @return 保存的题目数
     */
    @Transactional
    public int saveQuestions(List<SaveQuestionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("没有要保存的题目");
        }
        if (requests.size() > MAX_BATCH) {
            throw new IllegalArgumentException("一次最多保存 " + MAX_BATCH + " 道题");
        }

        Map<Long, Subject> subjects = loadById(requests.stream().map(SaveQuestionRequest::getSubjectId).toList(),
                subjectRepository::findAllById, Subject::getId, "学科");
        Map<Long, Chapter> chapters = loadById(requests.stream().map(SaveQuestionRequest::getChapterId).toList(),
                chapterRepository::findAllById, Chapter::getId, "章节");

        List<Question> questions = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            questions.add(toQuestion(requests.get(i), "第 " + (i + 1) + " 题：", subjects, chapters));
        }
        return questionRepository.saveAll(questions).size();
    }

    /** 教师在题库页手动录入一道题 */
    @Transactional
    public Question createQuestion(SaveQuestionRequest request) {
        return questionRepository.save(toQuestion(request));
    }

    /** 用请求内容覆盖已有题目；标题等请求里没有的字段保持不变 */
    @Transactional
    public Question updateQuestion(Long id, SaveQuestionRequest request) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("题目不存在"));
        Question updated = toQuestion(request);
        existing.setSubject(updated.getSubject());
        existing.setChapter(updated.getChapter());
        existing.setType(updated.getType());
        existing.setDifficulty(updated.getDifficulty());
        existing.setScore(updated.getScore());
        existing.setContent(updated.getContent());
        existing.setOptions(updated.getOptions());
        existing.setAnswer(updated.getAnswer());
        existing.setAnalysis(updated.getAnalysis());
        existing.setTags(updated.getTags());
        return questionRepository.save(existing);
    }

    private Question toQuestion(SaveQuestionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("题目内容为空");
        }
        Map<Long, Subject> subjects = loadById(Collections.singletonList(request.getSubjectId()),
                subjectRepository::findAllById, Subject::getId, "学科");
        Map<Long, Chapter> chapters = loadById(Collections.singletonList(request.getChapterId()),
                chapterRepository::findAllById, Chapter::getId, "章节");
        return toQuestion(request, "", subjects, chapters);
    }

    private static Question toQuestion(SaveQuestionRequest request, String prefix, Map<Long, Subject> subjects,
                                       Map<Long, Chapter> chapters) {
        if (request == null) {
            throw new IllegalArgumentException(prefix + "内容为空");
        }
        QuestionType type = QuestionType.parse(request.getType())
                .orElseThrow(() -> new IllegalArgumentException(prefix + "未知题型 " + request.getType()));
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new IllegalArgumentException(prefix + "题目内容不能为空");
        }
        if (type.isChoice()) {
            List<String> options = request.getOptions();
            if (options == null || options.size() < 2) {
                throw new IllegalArgumentException(prefix + "选择题至少需要两个选项");
            }
            if (options.stream().anyMatch(option -> option == null || option.isBlank())) {
                throw new IllegalArgumentException(prefix + "选项内容不能为空");
            }
        }
        String answer = QuestionTextNormalizer.normalizeAnswer(type, request.getAnswer());
        if (answer.isBlank()) {
            throw new IllegalArgumentException(prefix + "答案不能为空");
        }

        Chapter chapter = request.getChapterId() == null ? null : chapters.get(request.getChapterId());
        String subjectName;
        if (request.getSubjectId() != null) {
            subjectName = subjects.get(request.getSubjectId()).getName();
        } else if (chapter != null && chapter.getSubject() != null) {
            subjectName = chapter.getSubject().getName();
        } else if (request.getSubject() != null && !request.getSubject().isBlank()) {
            subjectName = request.getSubject().trim();
        } else {
            throw new IllegalArgumentException(prefix + "请选择学科");
        }
        if (chapter != null && chapter.getSubject() != null && !chapter.getSubject().getName().equals(subjectName)) {
            throw new IllegalArgumentException(prefix + "章节《" + chapter.getName() + "》不属于学科《" + subjectName + "》");
        }

        Integer difficulty = request.getDifficulty();
        if (difficulty == null || difficulty < 1 || difficulty > 5) {
            difficulty = DEFAULT_DIFFICULTY;
        }

        Question question = new Question();
        question.setSubject(subjectName);
        question.setChapter(chapter);
        question.setType(type);
        question.setDifficulty(difficulty);
        question.setScore(request.getScore() != null && request.getScore() > 0 ? request.getScore() : null);
        question.setContent(request.getContent().trim());
        question.setOptions(type.isChoice()
                ? new ArrayList<>(QuestionTextNormalizer.stripLetterPrefixes(request.getOptions()))
                : new ArrayList<>());
        question.setAnswer(answer);
        question.setAnalysis(request.getAnalysis() == null ? "" : request.getAnalysis().trim());
        question.setTags(request.getTags() == null ? new ArrayList<>() : new ArrayList<>(request.getTags()));
        return question;
    }

    /** 一次查出所有引用的学科或章节，有不存在的 ID 时直接报错 */
    private static <T> Map<Long, T> loadById(List<Long> rawIds, Function<List<Long>, List<T>> finder,
                                             Function<T, Long> idOf, String what) {
        List<Long> ids = rawIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        Map<Long, T> found = finder.apply(ids).stream().collect(Collectors.toMap(idOf, Function.identity()));
        for (Long id : ids) {
            if (!found.containsKey(id)) {
                throw new IllegalArgumentException(what + "不存在: " + id);
            }
        }
        return found;
    }
}
