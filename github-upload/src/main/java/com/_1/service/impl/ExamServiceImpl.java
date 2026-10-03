package com._1.service.impl;

import com._1.entity.ClassEntity;
import com._1.entity.Exam;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import com._1.entity.Subject;
import com._1.repository.ClassEntityRepository;
import com._1.repository.ExamRepository;
import com._1.repository.QuestionRepository;
import com._1.repository.SubjectRepository;
import com._1.service.ExamService;
import com._1.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class ExamServiceImpl implements ExamService {
    @Autowired
    private ExamRepository examRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;
    
    @Autowired
    private QuestionService questionService;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private ClassEntityRepository classRepository;

    @Override
    public List<Exam> findAll() {
        return examRepository.findAll();
    }
    
    @Override
    public Page<Exam> findAll(Pageable pageable) {
        return examRepository.findAll(pageable);
    }

    @Override
    public Optional<Exam> findById(Long id) {
        return examRepository.findById(id);
    }
    
    @Override
    public List<Exam> findBySubject(Subject subject) {
        return examRepository.findBySubject(subject);
    }
    
    @Override
    public List<Exam> findByTargetClass(ClassEntity classEntity) {
        return examRepository.findByTargetClass(classEntity);
    }
    
    @Override
    public List<Exam> findByExamDateBetween(Date startDate, Date endDate) {
        return examRepository.findByExamDateBetween(startDate, endDate);
    }
    
    @Override
    public Page<Exam> search(String keyword, Pageable pageable) {
        return examRepository.search(keyword, pageable);
    }

    @Override
    public Exam save(Exam exam) {
        return examRepository.save(exam);
    }

    @Override
    public void deleteById(Long id) {
        examRepository.deleteById(id);
    }
    
    @Override
    @Transactional
    public void addQuestionToExam(Long examId, Long questionId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("考试不存在: " + examId));
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + questionId));
        
        if (exam.getQuestions() == null) {
            exam.setQuestions(new ArrayList<>());
        }
        
        exam.getQuestions().add(question);
        examRepository.save(exam);
    }
    
    @Override
    @Transactional
    public void removeQuestionFromExam(Long examId, Long questionId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("考试不存在: " + examId));
        
        if (exam.getQuestions() != null) {
            exam.getQuestions().removeIf(q -> q.getId().equals(questionId));
            examRepository.save(exam);
        }
    }
    
    @Override
    @Transactional
    public void addClassToExam(Long examId, Long classId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("考试不存在: " + examId));
        ClassEntity classEntity = classRepository.findById(classId)
            .orElseThrow(() -> new IllegalArgumentException("班级不存在: " + classId));
        
        if (exam.getTargetClasses() == null) {
            exam.setTargetClasses(new ArrayList<>());
        }
        
        exam.getTargetClasses().add(classEntity);
        examRepository.save(exam);
    }
    
    @Override
    @Transactional
    public void removeClassFromExam(Long examId, Long classId) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("考试不存在: " + examId));
        
        if (exam.getTargetClasses() != null) {
            exam.getTargetClasses().removeIf(c -> c.getId().equals(classId));
            examRepository.save(exam);
        }
    }
    
    @Override
    @Transactional
    public Exam generateExam(Long subjectId, String examType, List<Long> chapterIds, 
                           int choiceCount, int fillBlankCount, int trueFalseCount, int shortAnswerCount) {
        Subject subject = subjectRepository.findById(subjectId)
            .orElseThrow(() -> new IllegalArgumentException("学科不存在: " + subjectId));
        
        Exam exam = new Exam();
        exam.setName(subject.getName() + " - " + 
                    (examType.equals("chapter") ? "章节测试" : 
                     examType.equals("midterm") ? "期中考试" : "期末考试"));
        exam.setSubject(subject);
        exam.setCreateTime(new Date());
        exam.setTotalScore(100); 
        
        List<Question> selectedQuestions = new ArrayList<>();
        
        if (choiceCount > 0) {
            List<Question> choiceQuestions = new ArrayList<>();
            if (examType.equals("chapter")) {
                choiceQuestions = questionService.findRandomQuestionsByChapterIn(chapterIds, choiceCount);
            } else if (examType.equals("midterm")) {
                choiceQuestions = questionService.findRandomQuestionsBySubjectAndType(
                    subject, QuestionType.SINGLE_CHOICE, choiceCount);
            } else {
                choiceQuestions = questionService.findRandomQuestionsBySubjectAndType(
                    subject, QuestionType.SINGLE_CHOICE, choiceCount);
            }
            selectedQuestions.addAll(choiceQuestions);
        }
        
        if (fillBlankCount > 0) {
            List<Question> fillBlankQuestions = new ArrayList<>();
            if (examType.equals("chapter")) {
                fillBlankQuestions = questionService.findRandomQuestionsByChapterIn(chapterIds, fillBlankCount);
            } else if (examType.equals("midterm")) {
                fillBlankQuestions = questionService.findRandomQuestionsBySubjectAndType(
                    subject, QuestionType.FILL_IN_THE_BLANK, fillBlankCount);
            } else {
                fillBlankQuestions = questionService.findRandomQuestionsBySubjectAndType(
                    subject, QuestionType.FILL_IN_THE_BLANK, fillBlankCount);
            }
            selectedQuestions.addAll(fillBlankQuestions);
        }
        
        if (trueFalseCount > 0) {
            List<Question> trueFalseQuestions = new ArrayList<>();
            if (examType.equals("chapter")) {
                trueFalseQuestions = questionService.findRandomQuestionsByChapterIn(chapterIds, trueFalseCount);
            } else if (examType.equals("midterm")) {
                trueFalseQuestions = questionService.findRandomQuestionsBySubjectAndType(
                    subject, QuestionType.TRUE_FALSE, trueFalseCount);
            } else {
                trueFalseQuestions = questionService.findRandomQuestionsBySubjectAndType(
                    subject, QuestionType.TRUE_FALSE, trueFalseCount);
            }
            selectedQuestions.addAll(trueFalseQuestions);
        }
        
        if (shortAnswerCount > 0) {
            List<Question> shortAnswerQuestions = new ArrayList<>();
            if (examType.equals("chapter")) {
                shortAnswerQuestions = questionService.findRandomQuestionsByChapterIn(chapterIds, shortAnswerCount);
            } else if (examType.equals("midterm")) {
                shortAnswerQuestions = questionService.findRandomQuestionsBySubjectAndType(
                    subject, QuestionType.SHORT_ANSWER, shortAnswerCount);
            } else {
                shortAnswerQuestions = questionService.findRandomQuestionsBySubjectAndType(
                    subject, QuestionType.SHORT_ANSWER, shortAnswerCount);
            }
            selectedQuestions.addAll(shortAnswerQuestions);
        }
        
        exam.setQuestions(selectedQuestions);
        return examRepository.save(exam);
    }
} 