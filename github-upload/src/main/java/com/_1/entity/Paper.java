package com._1.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "papers")
public class Paper {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;  // 试卷标题
    
    @Column
    private String description;  // 试卷描述
    
    @Column
    private String subject;  // 科目名称
    
    @Column
    private Integer duration;  // 考试时长(分钟)
    
    @Column
    private Integer totalScore;  // 总分
    
    @Column
    private Integer questionCount;  // 题目数量
    
    @Column
    private String createdBy;  // 创建人
    
    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;  // 创建时间
    
    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;  // 更新时间
    
    // 试卷题目关联表
    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PaperQuestion> paperQuestions;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
