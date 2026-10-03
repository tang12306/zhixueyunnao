package com._1.repository;

import com._1.entity.Paper;
import com._1.entity.PaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {
    
    // 根据试卷ID查找所有题目
    List<PaperQuestion> findByPaperIdOrderByOrderNum(Long paperId);
    
    // 根据试卷查找所有题目
    List<PaperQuestion> findByPaperOrderByOrderNum(Paper paper);
    
    // 删除试卷的所有题目
    void deleteByPaper(Paper paper);
    
    // 查询试卷的题目数量
    @Query("SELECT COUNT(pq) FROM PaperQuestion pq WHERE pq.paper.id = :paperId")
    Long countByPaperId(@Param("paperId") Long paperId);
    
    // 查询试卷的总分
    @Query("SELECT SUM(pq.score) FROM PaperQuestion pq WHERE pq.paper.id = :paperId")
    Integer sumScoreByPaperId(@Param("paperId") Long paperId);
}
