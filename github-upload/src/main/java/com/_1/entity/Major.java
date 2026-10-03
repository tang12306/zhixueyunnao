package com._1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "majors")
public class Major {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;  // 专业名称
    
    @Column
    private String code;  // 专业代码
    
    @Column
    private String description;  // 专业描述
    
    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    private College college;  // 所属学院
    
    @OneToMany(mappedBy = "major", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ClassEntity> classes;  // 专业下属班级（使用ClassEntity避免与Java关键字Class冲突）
    
    @ManyToMany
    @JoinTable(
        name = "major_subject", 
        joinColumns = @JoinColumn(name = "major_id"),
        inverseJoinColumns = @JoinColumn(name = "subject_id")
    )
    @JsonIgnore
    private List<Subject> subjects;  // 专业关联的学科
} 