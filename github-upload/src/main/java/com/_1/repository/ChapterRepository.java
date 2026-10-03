package com._1.repository;

import com._1.entity.Chapter;
import com._1.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findBySubjectOrderByOrderNumAsc(Subject subject);
    boolean existsByNameAndSubject(String name, Subject subject);
    Optional<Chapter> findByNameAndSubject(String name, Subject subject);
    
    /**
     * 查询所有章节并按科目ID和排序号排序
     */
    @Query("SELECT c FROM Chapter c JOIN c.subject s ORDER BY s.id, c.orderNum")
    List<Chapter> findAllOrderBySubjectAndOrderNum();
} 