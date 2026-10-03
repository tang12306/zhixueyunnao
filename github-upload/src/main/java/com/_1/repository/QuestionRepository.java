package com._1.repository;

import com._1.entity.Chapter;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long>, JpaSpecificationExecutor<Question> {
    @Query("SELECT q FROM Question q WHERE q.content LIKE %:keyword% OR q.answer LIKE %:keyword%")
    Page<Question> search(@Param("keyword") String keyword, Pageable pageable);
    
    List<Question> findByChapter(Chapter chapter);
    
    List<Question> findByChapterAndType(Chapter chapter, QuestionType type);
    
    List<Question> findByChapterIn(List<Chapter> chapters);
    
    List<Question> findByChapterInAndType(List<Chapter> chapters, QuestionType type);
    
    @Query(value = "SELECT * FROM questions WHERE chapter_id = :chapterId ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Question> findRandomQuestionsByChapter(@Param("chapterId") Long chapterId, @Param("count") int count);
    
    @Query(value = "SELECT * FROM questions WHERE chapter_id IN :chapterIds ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Question> findRandomQuestionsByChapterIn(@Param("chapterIds") List<Long> chapterIds, @Param("count") int count);
    
    Page<Question> findByChapter(Chapter chapter, Pageable pageable);
    
    Page<Question> findByChapterAndType(Chapter chapter, QuestionType type, Pageable pageable);
    
    Page<Question> findByChapterIn(List<Chapter> chapters, Pageable pageable);
    
    Page<Question> findByChapterInAndType(List<Chapter> chapters, QuestionType type, Pageable pageable);
    
    Page<Question> findByType(QuestionType type, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Question q JOIN q.chapter c JOIN c.subject s WHERE s.name = :subjectName")
    long countBySubject(@Param("subjectName") String subjectName);
} 