package com.example.service;

import com.example.entity.User;
import com.example.entity.Role;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void deleteById(Long id);
    void addRoleToUser(Long userId, Long roleId);
    void removeRoleFromUser(Long userId, Long roleId);
    boolean existsByUsername(String username);
} 