package com._1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "reflection_reports")
public class ReflectionReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对应的考试成绩记录 */
    @OneToOne
    @JoinColumn(name = "score_id")
    private Score score;

    /** 学生薄弱项分析 JSON 或纯文本 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String weaknessAnalysis;

    /** 错题分析 JSON 或纯文本 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String mistakeAnalysis;

    /** 正确答案解析 JSON 或纯文本 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String correctAnswerAnalysis;

    /** 针对每道错题的练习题（包含答案与解析）JSON 或纯文本 */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String practiceProblems;

    @Temporal(TemporalType.TIMESTAMP)
    private Date generatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
} 