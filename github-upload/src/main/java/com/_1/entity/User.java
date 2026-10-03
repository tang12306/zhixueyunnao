package com._1.entity;

import jakarta.persistence.*;
import java.util.Date; // Import Date for birthday
import java.util.Objects;
import lombok.Data; // Add Lombok import
import com.fasterxml.jackson.annotation.JsonProperty;

@Data // Add @Data annotation
@Entity
@Table(name = "users") // 数据库表名，如果你的表名不同，请修改这里
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // 学号将作为用户名

    @Column(nullable = false)
    private String password; // 存储加密后的密码

    private String name;     // 姓名

    @Column(nullable = false)
    private String role;     // 角色 (例如 "STUDENT", "TEACHER")

    @Column(nullable = false)
    private Boolean enabled = true;  // 用户是否启用，默认为true

    @ManyToOne(fetch = FetchType.EAGER) // Changed from LAZY to EAGER
    @JoinColumn(name = "class_id", referencedColumnName = "id") // 明确指定外键列名和引用列
    @JsonProperty("clazz") // JSON序列化时使用clazz作为字段名，与前端保持一致
    private ClassEntity studentClass; // 学生所属班级, role 为 STUDENT 时有效

    // Fields merged from Student entity
    private String gender;  // 性别

    @Temporal(TemporalType.DATE)
    private Date birthday;  // 出生日期

    private String phone;  // 电话

    private String email;  // 邮箱

    public User() {
    }

    public User(String username, String password, String name, String role) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.role = role;
    }
} 