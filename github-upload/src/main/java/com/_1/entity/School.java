package com._1.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "schools")
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;  // 学校名称
    
    @Column
    private String address;  // 学校地址
    
    @Column
    private String description;  // 学校描述
    
    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<College> colleges;  // 学校下属学院
} 