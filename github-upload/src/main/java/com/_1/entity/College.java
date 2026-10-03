package com._1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "colleges")
public class College {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;  // 学院名称
    
    @Column
    private String description;  // 学院描述
    
    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;  // 所属学校
    
    @OneToMany(mappedBy = "college", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Major> majors;  // 学院下属专业
} 