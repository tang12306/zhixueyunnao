package com._1.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "paper_questions")
public class PaperQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;  // 所属试卷
    
    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;  // 题目
    
    @Column(nullable = false)
    private Integer score;  // 该题在试卷中的分数
    
    @Column
    private Integer orderNum;  // 题目在试卷中的顺序
    
    public PaperQuestion() {}
    
    public PaperQuestion(Paper paper, Question question, Integer score, Integer orderNum) {
        this.paper = paper;
        this.question = question;
        this.score = score;
        this.orderNum = orderNum;
    }
}
