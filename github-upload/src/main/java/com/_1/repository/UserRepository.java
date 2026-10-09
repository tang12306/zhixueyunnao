package com._1.repository;

import com._1.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByRole(String role);
    long countByRole(String role);
    boolean existsByRole(String role);
    List<User> findByStudentClassIdAndRole(Long classId, String role);
    List<User> findByRoleAndStudentClassIsNull(String role);

    // 新增：计算班级下的学生数量
    long countByStudentClassIdAndRole(Long classId, String role);
}