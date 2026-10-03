package com._1.service;

import com._1.entity.Paper;
import com._1.entity.PaperQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PaperService {
    
    // 基本CRUD操作
    Paper save(Paper paper);
    Optional<Paper> findById(Long id);
    List<Paper> findAll();
    Page<Paper> findAll(Pageable pageable);
    void deleteById(Long id);
    
    // 查询操作
    List<Paper> findByCreatedBy(String createdBy);
    List<Paper> findBySubject(String subject);
    Page<Paper> findByCreatedBy(String createdBy, Pageable pageable);
    Page<Paper> findPapersWithFilters(String createdBy, String subject, String keyword, Pageable pageable);
    
    // 试卷题目管理
    List<PaperQuestion> getPaperQuestions(Long paperId);
    Paper createPaperWithQuestions(String title, String description, String subject, 
                                  Integer duration, String createdBy, 
                                  List<Map<String, Object>> questionsList);
    
    // 统计信息
    Long countQuestionsByPaperId(Long paperId);
    Integer sumScoreByPaperId(Long paperId);
    
    // Word导出
    byte[] exportToWord(Long paperId) throws Exception;
}
