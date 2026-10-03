package com._1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "scores")
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Double score;  // 分数
    
    @Column(name = "student_rank")
    private Integer rank;  // 排名(在班级内)
    
    @Column
    private String comment;  // 评语
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;  // 考试
    
    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;  // 录入时间
    
    @Column
    private String creator;  // 录入人
} 