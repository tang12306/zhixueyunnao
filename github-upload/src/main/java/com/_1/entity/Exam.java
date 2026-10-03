package com._1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;  // 考试名称
    
    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date examDate;  // 考试日期
    
    @Transient // This field is not persisted to the database
    private String examDateStr; // For datetime-local input binding
    
    @Column
    private Integer duration;  // 考试时长(分钟)
    
    @Column
    private String examType;  // 考试类型(期中/期末/测验)
    
    @Column
    private Integer totalScore;  // 总分
    
    @Column
    private String status;  // 考试状态(未开始/进行中/已结束)
    
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;  // 考试科目
    
    @ManyToMany
    @JoinTable(
        name = "exam_class", 
        joinColumns = @JoinColumn(name = "exam_id"),
        inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    private List<ClassEntity> targetClasses;  // 考试目标班级
    
    @ManyToMany
    @JoinTable(
        name = "exam_question", 
        joinColumns = @JoinColumn(name = "exam_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    private List<Question> questions;  // 考试题目
    
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL)
    private List<Score> scores;  // 考试成绩
    
    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime;  // 创建时间
    
    @Column
    private String creator;  // 创建人

    public String getExamDateStr() {
        if (this.examDate != null) {
            // Format java.util.Date to yyyy-MM-ddTHH:mm string
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            return sdf.format(this.examDate);
        }
        return examDateStr; // Return the manually set string if date is null, or let it be null
    }

    public void setExamDateStr(String examDateStr) {
        this.examDateStr = examDateStr;
        if (examDateStr != null && !examDateStr.isEmpty()) {
            try {
                // Parse yyyy-MM-ddTHH:mm string to java.util.Date
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
                this.examDate = sdf.parse(examDateStr);
            } catch (java.text.ParseException e) {
                // Log error or handle appropriately
                System.err.println("Error parsing examDateStr: " + examDateStr + "; " + e.getMessage());
                this.examDate = null; // Or throw an exception, or set a default
            }
        } else {
            this.examDate = null; // if string is empty, set date to null
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExamType() { return examType; }
    public void setExamType(String examType) { this.examType = examType; }
    public Date getExamDate() { return examDate; }
    public void setExamDate(Date examDate) { this.examDate = examDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }
    public java.util.List<ClassEntity> getTargetClasses() { return targetClasses; }
    public void setTargetClasses(java.util.List<ClassEntity> targetClasses) { this.targetClasses = targetClasses; }
    public java.util.List<Question> getQuestions() { return questions; }
    public void setQuestions(java.util.List<Question> questions) { this.questions = questions; }
    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }
} 