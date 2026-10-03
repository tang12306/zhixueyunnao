package com._1.service;

import com._1.dto.StudentData;
import com._1.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> findAll();
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    User save(User user);
    void deleteById(Long id);
    void resetPassword(Long id, String newPassword);
    boolean changePassword(String username, String currentPassword, String newPassword);
    User saveStudent(String username, String name, String rawPassword);
    void batchSaveStudents(List<StudentData> studentDataList);
    User saveUser(String username, String name, String rawPassword, String role);

    // 新增方法，用于管理班级下的学生
    List<User> findStudentsByClazzId(Long classId);
    User saveStudentInClass(User student, Long classId); // 创建或更新学生并关联到班级
    User updateStudentInClass(User studentDetails, Long classId); // 更新学生信息，确保仍在班级中

    List<User> findUsersByRoleAndClassEntityIsNull(String role); // 新增方法声明

    void assignStudentToClass(Long studentId, Long classId); // 分配学生到班级
} 