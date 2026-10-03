package com._1.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "chapters")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    private Integer orderNum;
    
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;
    
    @OneToMany(mappedBy = "chapter", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Question> questions = new ArrayList<>();
} 