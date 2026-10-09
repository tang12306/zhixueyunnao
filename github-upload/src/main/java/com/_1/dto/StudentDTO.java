package com._1.dto;

import com._1.entity.ClassEntity;
import com._1.entity.User;

import java.time.LocalDate;

/**
 * 学生管理接口的返回值。只包含页面用到的字段，不含密码哈希。
 * clazz 与原先直接返回 User 实体时的字段名保持一致，前端无需改动。
 */
public record StudentDTO(
        Long id,
        String username,
        String name,
        String gender,
        LocalDate birthday,
        String phone,
        String email,
        Boolean enabled,
        ClassRef clazz) {

    public record ClassRef(Long id, String name) {
    }

    public static StudentDTO from(User user) {
        ClassEntity c = user.getStudentClass();
        LocalDate birthday = user.getBirthday() == null
                ? null
                : new java.sql.Date(user.getBirthday().getTime()).toLocalDate();
        return new StudentDTO(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getGender(),
                birthday,
                user.getPhone(),
                user.getEmail(),
                user.getEnabled(),
                c == null ? null : new ClassRef(c.getId(), c.getName()));
    }
}
