/*
package com._1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String studentId;  // 学号
    
    @Column(nullable = false)
    private String name;  // 学生姓名
    
    @Column
    private String gender;  // 性别
    
    @Temporal(TemporalType.DATE)
    private Date birthday;  // 出生日期
    
    @Column
    private String phone;  // 电话
    
    @Column
    private String email;  // 邮箱
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity studentClass;  // 所属班级
    
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<Score> scores;  // 学生成绩

    public Student() {
    }

    public Student(String studentId, String name) {
        this.studentId = studentId;
        this.name = name;
    }
}
*/ 