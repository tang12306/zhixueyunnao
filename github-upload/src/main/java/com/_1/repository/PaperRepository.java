package com._1.repository;

import com._1.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {
    
    // 根据创建人查找试卷
    List<Paper> findByCreatedBy(String createdBy);
    
    // 根据科目查找试卷
    List<Paper> findBySubject(String subject);
    
    // 根据标题模糊查询
    List<Paper> findByTitleContainingIgnoreCase(String title);
    
    // 分页查询试卷
    Page<Paper> findByCreatedBy(String createdBy, Pageable pageable);
    
    // 根据科目分页查询
    Page<Paper> findBySubject(String subject, Pageable pageable);
    
    // 复合查询：根据创建人和科目
    @Query("SELECT p FROM Paper p WHERE " +
           "(:createdBy IS NULL OR p.createdBy = :createdBy) AND " +
           "(:subject IS NULL OR p.subject = :subject) AND " +
           "(:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Paper> findPapersWithFilters(@Param("createdBy") String createdBy,
                                     @Param("subject") String subject,
                                     @Param("keyword") String keyword,
                                     Pageable pageable);
}
