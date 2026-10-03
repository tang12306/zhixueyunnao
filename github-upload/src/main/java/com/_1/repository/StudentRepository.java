/*
package com._1.repository;

import com._1.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentId(String studentId);
    List<Student> findByStudentClass_Id(Long classId); // 根据班级ID查询学生
    // You can add more custom query methods here if needed
}
*/ 