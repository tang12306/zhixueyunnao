package com._1.repository;

import com._1.entity.College;
import com._1.entity.Major;
import com._1.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    List<Major> findByCollege(College college);
    boolean existsByNameAndCollege(String name, College college);
    Optional<Major> findByNameAndCollege(String name, College college);
    List<Major> findBySubjectsContaining(Subject subject);
    
    @Query("SELECT m FROM Major m JOIN m.subjects s WHERE s.id = :subjectId")
    List<Major> findBySubjectId(@Param("subjectId") Long subjectId);

    List<Major> findByCollegeId(Long collegeId);
    boolean existsByNameAndCollegeId(String name, Long collegeId);
    Optional<Major> findByNameAndCollegeId(String name, Long collegeId);
} 