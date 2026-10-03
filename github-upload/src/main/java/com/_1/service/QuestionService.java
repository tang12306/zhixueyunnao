package com._1.service;

import com._1.entity.Chapter;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface QuestionService {
    List<Question> findAll();
    Page<Question> findAll(Pageable pageable);
    Optional<Question> findById(Long id);
    List<Question> findAllById(Iterable<Long> ids);
    Question save(Question question);
    List<Question> saveAll(List<Question> questions);
    void deleteById(Long id);
    Page<Question> search(String keyword, Pageable pageable);
    
    // 根据章节查询题目
    List<Question> findByChapter(Chapter chapter);
    
    // 根据章节和题型查询题目
    List<Question> findByChapterAndType(Chapter chapter, QuestionType type);
    
    // 根据学科查询题目（通过章节关联）
    List<Question> findBySubject(Subject subject);
    
    // 根据学科和题型查询题目
    List<Question> findBySubjectAndType(Subject subject, QuestionType type);
    
    // 随机获取指定数量的题目（按章节）
    List<Question> findRandomQuestionsByChapter(Long chapterId, int count);
    
    // 随机获取指定数量的题目（按章节列表）
    List<Question> findRandomQuestionsByChapterIn(List<Long> chapterIds, int count);
    
    // 随机获取指定数量的题目（按学科，用于期末考试）
    List<Question> findRandomQuestionsBySubject(Subject subject, int count);
    
    // 随机获取指定数量的题目（按学科和题型）
    List<Question> findRandomQuestionsBySubjectAndType(Subject subject, QuestionType type, int count);
    
    // 分页查询方法
    Page<Question> findAllByChapter(Chapter chapter, Pageable pageable);
    Page<Question> findAllByChapterAndType(Chapter chapter, QuestionType type, Pageable pageable);
    Page<Question> findAllBySubject(Subject subject, Pageable pageable);
    Page<Question> findAllBySubjectAndType(Subject subject, QuestionType type, Pageable pageable);
    Page<Question> findAllByType(QuestionType type, Pageable pageable);

    // 新增：按多种条件组合查询题目
    Page<Question> findByCriteria(Long subjectId, Long chapterId, QuestionType type, Integer difficulty, String keyword, Pageable pageable);

    long countBySubjectName(String subjectName);
} 