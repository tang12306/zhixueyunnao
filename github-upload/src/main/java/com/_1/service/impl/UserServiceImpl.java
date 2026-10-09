package com._1.service.impl;

import com._1.dto.StudentData;
import com._1.entity.ClassEntity;
import com._1.entity.User;
import com._1.repository.ClassEntityRepository;
import com._1.repository.UserRepository;
import com._1.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClassEntityRepository classEntityRepository;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, ClassEntityRepository classEntityRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.classEntityRepository = classEntityRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Attempting to load user by username: {}", username);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("User not found with username: {}", username);
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        User user = userOpt.get();
        logger.info("User found: {}, Role: {}", user.getUsername(), user.getRole());

        String role = user.getRole();
        // 确保角色名以 "ROLE_" 开头，这是Spring Security的约定
        if (role != null && !role.startsWith("ROLE_")) {
            role = "ROLE_" + role.toUpperCase(); // 转为大写并添加前缀
        }

        GrantedAuthority authority = new SimpleGrantedAuthority(role);

        boolean enabled = !Boolean.FALSE.equals(user.getEnabled());
        return new org.springframework.security.core.userdetails.User(user.getUsername(),
                user.getPassword(), // 数据库中存储的应该是已加密的密码
                enabled, true, true, true,
                Collections.singletonList(authority));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional
    public User saveStudent(String username, String name, String rawPassword) {
        Optional<User> existingUserOpt = userRepository.findByUsername(username);
        if (existingUserOpt.isPresent()) {
            logger.info("User with username {} already exists. Returning existing user.", username);
            return existingUserOpt.get(); 
        }
        User studentEntity = new User();
        studentEntity.setUsername(username);
        studentEntity.setName(name);
        studentEntity.setPassword(passwordEncoder.encode(rawPassword)); // 加密密码
        studentEntity.setRole("STUDENT"); // 存储为 "STUDENT", loadUserByUsername 会处理 "ROLE_" 前缀
        logger.info("Saving new student: username={}, name={}", username, name);
        return userRepository.save(studentEntity);
    }

    @Override
    @Transactional
    public void batchSaveStudents(List<StudentData> studentDataList) {
        if (studentDataList == null || studentDataList.isEmpty()) {
            logger.info("Student data list is empty. No students to save.");
            return;
        }
        logger.info("Starting batch save for {} students.", studentDataList.size());
        for (StudentData data : studentDataList) {
            if (data.getId() == null || data.getName() == null) {
                logger.warn("Skipping student data with null id or name: {}", data);
                continue;
            }
            // 使用学号作为初始密码，角色为STUDENT
            saveUser(data.getId(), data.getName(), data.getId(), "STUDENT");
        }
        logger.info("Batch student saving process completed.");
    }

    @Transactional // 新增 saveUser 方法的实现
    @Override
    public User saveUser(String username, String name, String rawPassword, String role) {
        Optional<User> existingUserOpt = userRepository.findByUsername(username);
        if (existingUserOpt.isPresent()) { 
            logger.info("User with username {} already exists. Returning existing user.", username);
            return existingUserOpt.get();
        }
        User userEntity = new User();
        userEntity.setUsername(username);
        userEntity.setName(name);
        userEntity.setPassword(passwordEncoder.encode(rawPassword));
        userEntity.setRole(role); 
        logger.info("Saving new user: username={}, name={}, role={}", username, name, role);
        return userRepository.save(userEntity);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 原样保存，不处理密码。设置新密码一律走 encodePassword / resetPassword / changePassword，
     * 不再靠 "$2a$" 前缀猜测密码是否已加密（那样调用方可以直接写入一个现成的哈希）。
     */
    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRole(String role) {
        return userRepository.countByRole(role);
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        userRepository.findById(id).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        });
    }
    
    @Override
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        Optional<User> userOpt = this.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 验证当前密码是否正确
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                // 更新密码
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        
        return false;
    }

    // --- 新增和修改的方法，用于管理班级学生 ---

    @Override
    @Transactional(readOnly = true)
    public List<User> findStudentsByClazzId(Long classId) {
        return userRepository.findByStudentClassIdAndRole(classId, "STUDENT");
    }

    @Override
    @Transactional
    public User saveStudentInClass(User studentFormData, Long classId) {
        if (studentFormData.getUsername() == null || studentFormData.getUsername().isEmpty()) {
            throw new IllegalArgumentException("学生学号 (username) 不能为空");
        }

        ClassEntity clazz = classEntityRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("无效的班级ID: " + classId));

        User userToSave;

        if (studentFormData.getId() == null) { 
            Optional<User> existingUserOpt = userRepository.findByUsername(studentFormData.getUsername());
            if (existingUserOpt.isPresent()) { 
                User existingUserByUsername = existingUserOpt.get();
                if ("STUDENT".equals(existingUserByUsername.getRole()) && existingUserByUsername.getStudentClass() == null) {
                    logger.info("认领已存在的未分配班级的学生: {}", studentFormData.getUsername());
                    userToSave = existingUserByUsername;
                    userToSave.setName(studentFormData.getName()); 
                    if (studentFormData.getPassword() != null && !studentFormData.getPassword().isEmpty()) {
                        userToSave.setPassword(passwordEncoder.encode(studentFormData.getPassword()));
                    }
                } else {
                    throw new IllegalArgumentException("学号 " + studentFormData.getUsername() + " 已被占用或不适用。请检查是否已在其他班级或角色不符。");
                }
            } else {
                logger.info("创建新学生用户: {}", studentFormData.getUsername());
                userToSave = new User();
                userToSave.setUsername(studentFormData.getUsername());
                userToSave.setName(studentFormData.getName());
                userToSave.setRole("STUDENT");
                if (studentFormData.getPassword() != null && !studentFormData.getPassword().isEmpty()) {
                    userToSave.setPassword(passwordEncoder.encode(studentFormData.getPassword()));
                } else {
                    userToSave.setPassword(passwordEncoder.encode(studentFormData.getUsername()));
                }
            }
        } else { 
            logger.info("更新现有学生ID: {}", studentFormData.getId());
            userToSave = userRepository.findById(studentFormData.getId())
                    .orElseThrow(() -> new IllegalArgumentException("更新失败：找不到ID为 " + studentFormData.getId() + " 的学生"));
            if (!"STUDENT".equals(userToSave.getRole())) {
                throw new IllegalArgumentException("用户 " + userToSave.getUsername() + " 不是学生角色，无法在班级中更新。");
            }
            userToSave.setName(studentFormData.getName());
            if (studentFormData.getUsername() != null && !studentFormData.getUsername().equals(userToSave.getUsername())) {
                if (userRepository.findByUsername(studentFormData.getUsername()).isPresent()) { // Check Optional
                    throw new IllegalArgumentException("新的学号 " + studentFormData.getUsername() + " 已被其他用户占用。");
                }
                userToSave.setUsername(studentFormData.getUsername());
            }
            if (studentFormData.getPassword() != null && !studentFormData.getPassword().isEmpty()) {
                userToSave.setPassword(passwordEncoder.encode(studentFormData.getPassword()));
            }
        }

        userToSave.setStudentClass(clazz);
        return userRepository.save(userToSave);
    }

    @Override
    @Transactional
    public User updateStudentInClass(User studentDetails, Long classId) {
        if (studentDetails.getId() == null) {
            throw new IllegalArgumentException("要更新的学生必须提供ID");
        }
        User existingStudent = userRepository.findById(studentDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("未找到ID为 " + studentDetails.getId() + " 的学生"));

        if (!"STUDENT".equals(existingStudent.getRole())) {
            throw new IllegalArgumentException("用户 " + existingStudent.getUsername() + " 不是学生角色，无法在班级中更新。");
        }

        ClassEntity clazz = classEntityRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("无效的班级ID: " + classId));

        // 更新允许修改的字段
        existingStudent.setName(studentDetails.getName());
        // 如果提供了新的学号 (username), 并且与现有不同，需要检查是否已存在
        if (studentDetails.getUsername() != null && !studentDetails.getUsername().equals(existingStudent.getUsername())) {
            if (userRepository.findByUsername(studentDetails.getUsername()).isPresent()) {
                throw new IllegalArgumentException("新的学号 " + studentDetails.getUsername() + " 已被其他用户占用。");
            }
            existingStudent.setUsername(studentDetails.getUsername());
        }
        
        // 密码更新逻辑 (可选，如果表单提供密码字段)
        if (studentDetails.getPassword() != null && !studentDetails.getPassword().isEmpty()) {
            existingStudent.setPassword(passwordEncoder.encode(studentDetails.getPassword()));
        }

        existingStudent.setStudentClass(clazz); // 确保班级关联正确

        logger.info("Updating student {} in class {}", existingStudent.getUsername(), clazz.getName());
        return userRepository.save(existingStudent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findUsersByRoleAndClassEntityIsNull(String role) {
        return userRepository.findByRoleAndStudentClassIsNull(role);
    }

    @Override
    @Transactional
    public void assignStudentToClass(Long studentId, Long classId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("无效的学生ID: " + studentId));
        if (!"STUDENT".equals(student.getRole())) {
            throw new IllegalArgumentException("用户 " + student.getUsername() + " 不是学生角色，不能分配到班级。");
        }
        if (student.getStudentClass() != null) {
            throw new IllegalArgumentException("学生 " + student.getUsername() + " 已分配到班级: " + student.getStudentClass().getName());
        }
        ClassEntity clazz = classEntityRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("无效的班级ID: " + classId));
        
        student.setStudentClass(clazz);
        userRepository.save(student);
        logger.info("学生 {} ({}) 已成功分配到班级 {} ({})", student.getName(), student.getUsername(), clazz.getName(), clazz.getId());
    }

    // 考虑修改 batchSaveStudents 以支持班级ID
    // public void batchSaveStudents(List<StudentData> studentDataList, Long classId) { ... }

} 