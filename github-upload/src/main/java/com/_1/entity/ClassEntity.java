package com._1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "classes")
public class ClassEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;  // 班级名称
    
    @Column
    private Integer grade;  // 年级
    
    @Column
    private Integer studentCount;  // 学生人数
    
    @ManyToOne
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;  // 所属专业
    
    @OneToMany(mappedBy = "studentClass", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<User> students;  // 班级学生 (User实体中role为STUDENT的)
    
    @ManyToMany(mappedBy = "targetClasses")
    @JsonIgnore
    private List<Exam> exams;  // 班级关联的考试
} 