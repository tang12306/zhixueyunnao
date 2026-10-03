package com._1.dto;

import com._1.entity.Question;

public class QuestionAnswerDetail {
    private Question question;
    private String studentAnswer;
    private String correctAnswer;
    private boolean isCorrect;
    private double questionScore; // Score for this specific question

    public QuestionAnswerDetail(Question question, String studentAnswer, String correctAnswer, boolean isCorrect, double questionScore) {
        this.question = question;
        this.studentAnswer = studentAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
        this.questionScore = questionScore;
    }

    // Getters
    public Question getQuestion() {
        return question;
    }

    public String getStudentAnswer() {
        return studentAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public double getQuestionScore() {
        return questionScore;
    }

    // Setters (optional, depending on whether these details can be modified after creation)
    public void setQuestion(Question question) {
        this.question = question;
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }

    public void setQuestionScore(double questionScore) {
        this.questionScore = questionScore;
    }
} 