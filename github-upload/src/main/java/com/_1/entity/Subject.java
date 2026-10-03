package com._1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "subjects")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    private String description;
    
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Chapter> chapters = new ArrayList<>();
    
    @ManyToMany(mappedBy = "subjects")
    private List<Major> majors = new ArrayList<>();
    
    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL)
    private List<Exam> exams = new ArrayList<>();
} 