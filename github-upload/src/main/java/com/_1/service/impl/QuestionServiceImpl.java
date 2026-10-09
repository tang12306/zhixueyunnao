package com._1.service.impl;

import com._1.entity.Chapter;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.repository.ChapterRepository;
import com._1.repository.QuestionRepository;
import com._1.service.QuestionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final ChapterRepository chapterRepository;

    public QuestionServiceImpl(QuestionRepository questionRepository, ChapterRepository chapterRepository) {
        this.questionRepository = questionRepository;
        this.chapterRepository = chapterRepository;
    }

    @Override
    public List<Question> findAll() {
        return questionRepository.findAll();
    }

    @Override
    public Page<Question> findAll(Pageable pageable) {
        return questionRepository.findAll(pageable);
    }

    @Override
    public Optional<Question> findById(Long id) {
        return questionRepository.findById(id);
    }

    @Override
    public List<Question> findAllById(Iterable<Long> ids) {
        return questionRepository.findAllById(ids);
    }

    @Override
    public Question save(Question question) {
        return questionRepository.save(question);
    }

    @Override
    public void deleteById(Long id) {
        questionRepository.deleteById(id);
    }

    @Override
    public Page<Question> search(String keyword, Pageable pageable) {
        return questionRepository.search(keyword, pageable);
    }
    
    @Override
    public List<Question> findByChapter(Chapter chapter) {
        return questionRepository.findByChapter(chapter);
    }
    
    @Override
    public List<Question> findByChapterAndType(Chapter chapter, QuestionType type) {
        return questionRepository.findByChapterAndType(chapter, type);
    }
    
    @Override
    public List<Question> findBySubject(Subject subject) {
        List<Chapter> chapters = chapterRepository.findBySubjectOrderByOrderNumAsc(subject);
        return questionRepository.findByChapterIn(chapters);
    }
    
    @Override
    public List<Question> findBySubjectAndType(Subject subject, QuestionType type) {
        List<Chapter> chapters = chapterRepository.findBySubjectOrderByOrderNumAsc(subject);
        return questionRepository.findByChapterInAndType(chapters, type);
    }
    
    @Override
    public List<Question> findRandomQuestionsByChapter(Long chapterId, int count) {
        return questionRepository.findRandomQuestionsByChapter(chapterId, count);
    }
    
    @Override
    public List<Question> findRandomQuestionsByChapterIn(List<Long> chapterIds, int count) {
        return questionRepository.findRandomQuestionsByChapterIn(chapterIds, count);
    }
    
    @Override
    public List<Question> findRandomQuestionsBySubject(Subject subject, int count) {
        List<Chapter> chapters = chapterRepository.findBySubjectOrderByOrderNumAsc(subject);
        List<Long> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());
        return questionRepository.findRandomQuestionsByChapterIn(chapterIds, count);
    }
    
    @Override
    public List<Question> findRandomQuestionsBySubjectAndType(Subject subject, QuestionType type, int count) {
        List<Chapter> chapters = chapterRepository.findBySubjectOrderByOrderNumAsc(subject);
        List<Question> questions = questionRepository.findByChapterInAndType(chapters, type);
        List<Question> result = new ArrayList<>();
        if (!questions.isEmpty()) {
            if (questions.size() <= count) {
                return questions;
            }
            java.util.Random random = new java.util.Random();
            while (result.size() < count && !questions.isEmpty()) {
                int index = random.nextInt(questions.size());
                result.add(questions.remove(index));
            }
        }
        return result;
    }
    
    @Override
    public Page<Question> findAllByChapter(Chapter chapter, Pageable pageable) {
        return questionRepository.findByChapter(chapter, pageable);
    }
    
    @Override
    public Page<Question> findAllByChapterAndType(Chapter chapter, QuestionType type, Pageable pageable) {
        return questionRepository.findByChapterAndType(chapter, type, pageable);
    }
    
    @Override
    public Page<Question> findAllBySubject(Subject subject, Pageable pageable) {
        List<Chapter> chapters = chapterRepository.findBySubjectOrderByOrderNumAsc(subject);
        return questionRepository.findByChapterIn(chapters, pageable);
    }
    
    @Override
    public Page<Question> findAllBySubjectAndType(Subject subject, QuestionType type, Pageable pageable) {
        List<Chapter> chapters = chapterRepository.findBySubjectOrderByOrderNumAsc(subject);
        return questionRepository.findByChapterInAndType(chapters, type, pageable);
    }
    
    @Override
    public Page<Question> findAllByType(QuestionType type, Pageable pageable) {
        return questionRepository.findByType(type, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Question> findByCriteria(Long subjectId, Long chapterId, QuestionType type, Integer difficulty, String keyword, Pageable pageable) {
        return questionRepository.findAll((Specification<Question>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 按学科筛选：有章节的看章节所属学科；没有章节的题目（例如 AI 按多个章节出题后保存的）只记了学科名称
            if (subjectId != null) {
                Join<Question, Chapter> chapterJoin = root.join("chapter", JoinType.LEFT);
                Subquery<String> subjectName = query.subquery(String.class);
                Root<Subject> subjectRoot = subjectName.from(Subject.class);
                subjectName.select(subjectRoot.get("name")).where(criteriaBuilder.equal(subjectRoot.get("id"), subjectId));
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.equal(chapterJoin.get("subject").get("id"), subjectId),
                        criteriaBuilder.and(
                                criteriaBuilder.isNull(chapterJoin.get("id")),
                                criteriaBuilder.equal(root.get("subject"), subjectName))));
            }

            // Filter by Chapter ID
            if (chapterId != null) {
                predicates.add(criteriaBuilder.equal(root.get("chapter").get("id"), chapterId));
            }

            // Filter by Question Type
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), type));
            }

            // Filter by Difficulty
            if (difficulty != null) {
                predicates.add(criteriaBuilder.equal(root.get("difficulty"), difficulty));
            }

            // Filter by Keyword (in content, question's subject string, and analysis)
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase().trim() + "%";
                Predicate keywordInContent = criteriaBuilder.like(criteriaBuilder.lower(root.get("content")), likePattern);
                Predicate keywordInSubjectString = criteriaBuilder.like(criteriaBuilder.lower(root.get("subject")), likePattern);
                Predicate keywordInAnalysis = criteriaBuilder.like(criteriaBuilder.lower(root.get("analysis")), likePattern);
                // TODO: Add search in tags if tags are stored as a searchable collection/field
                predicates.add(criteriaBuilder.or(keywordInContent, keywordInSubjectString, keywordInAnalysis));
            }
            
            // Default sort order by ID descending if no sort is specified in pageable
            // This helps with consistent pagination
            if (pageable.getSort().isUnsorted()) {
                 query.orderBy(criteriaBuilder.desc(root.get("id")));
            } // else, the sort from pageable will be used by Spring Data JPA by default

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    @Override
    public List<Question> saveAll(List<Question> questions) {
        return questionRepository.saveAll(questions);
    }

    @Override
    public long countBySubjectName(String subjectName) {
        return questionRepository.countBySubject(subjectName);
    }
} 