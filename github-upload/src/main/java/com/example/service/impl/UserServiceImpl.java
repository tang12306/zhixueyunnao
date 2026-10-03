package com.example.service.impl;

import com.example.entity.User;
import com.example.entity.Role;
import com.example.repository.UserRepository;
import com.example.repository.RoleRepository;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User save(User user) {
        // 加密密码
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void addRoleToUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
} 