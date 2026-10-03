package com._1.service.impl;

import com._1.entity.Paper;
import com._1.entity.PaperQuestion;
import com._1.entity.Question;
import com._1.repository.PaperRepository;
import com._1.repository.PaperQuestionRepository;
import com._1.service.PaperService;
import com._1.service.QuestionService;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
public class PaperServiceImpl implements PaperService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaperServiceImpl.class);
    
    @Autowired
    private PaperRepository paperRepository;
    
    @Autowired
    private PaperQuestionRepository paperQuestionRepository;
    
    @Autowired
    private QuestionService questionService;
    
    @Override
    public Paper save(Paper paper) {
        return paperRepository.save(paper);
    }
    
    @Override
    public Optional<Paper> findById(Long id) {
        return paperRepository.findById(id);
    }
    
    @Override
    public List<Paper> findAll() {
        return paperRepository.findAll();
    }
    
    @Override
    public Page<Paper> findAll(Pageable pageable) {
        return paperRepository.findAll(pageable);
    }
    
    @Override
    public void deleteById(Long id) {
        paperRepository.deleteById(id);
    }
    
    @Override
    public List<Paper> findByCreatedBy(String createdBy) {
        return paperRepository.findByCreatedBy(createdBy);
    }
    
    @Override
    public List<Paper> findBySubject(String subject) {
        return paperRepository.findBySubject(subject);
    }
    
    @Override
    public Page<Paper> findByCreatedBy(String createdBy, Pageable pageable) {
        return paperRepository.findByCreatedBy(createdBy, pageable);
    }
    
    @Override
    public Page<Paper> findPapersWithFilters(String createdBy, String subject, String keyword, Pageable pageable) {
        return paperRepository.findPapersWithFilters(createdBy, subject, keyword, pageable);
    }
    
    @Override
    public List<PaperQuestion> getPaperQuestions(Long paperId) {
        return paperQuestionRepository.findByPaperIdOrderByOrderNum(paperId);
    }
    
    @Override
    public Paper createPaperWithQuestions(String title, String description, String subject, 
                                         Integer duration, String createdBy, 
                                         List<Map<String, Object>> questionsList) {
        try {
            // 创建试卷
            Paper paper = new Paper();
            paper.setTitle(title);
            paper.setDescription(description);
            paper.setSubject(subject);
            paper.setDuration(duration);
            paper.setCreatedBy(createdBy);
            paper.setQuestionCount(questionsList.size());
            
            // 计算总分
            int totalScore = 0;
            for (Map<String, Object> questionItem : questionsList) {
                Object scoreObj = questionItem.get("score");
                if (scoreObj != null) {
                    totalScore += Integer.valueOf(scoreObj.toString());
                }
            }
            paper.setTotalScore(totalScore);
            
            // 保存试卷
            Paper savedPaper = paperRepository.save(paper);
            
            // 创建试卷题目关联
            List<PaperQuestion> paperQuestions = new ArrayList<>();
            for (int i = 0; i < questionsList.size(); i++) {
                Map<String, Object> questionItem = questionsList.get(i);
                Long questionId = Long.valueOf(questionItem.get("questionId").toString());
                Integer score = Integer.valueOf(questionItem.get("score").toString());
                
                Optional<Question> questionOpt = questionService.findById(questionId);
                if (questionOpt.isPresent()) {
                    PaperQuestion paperQuestion = new PaperQuestion();
                    paperQuestion.setPaper(savedPaper);
                    paperQuestion.setQuestion(questionOpt.get());
                    paperQuestion.setScore(score);
                    paperQuestion.setOrderNum(i + 1);
                    paperQuestions.add(paperQuestion);
                }
            }
            
            // 保存试卷题目关联
            paperQuestionRepository.saveAll(paperQuestions);
            
            logger.info("试卷创建成功: ID={}, 标题={}, 题目数量={}, 总分={}", 
                       savedPaper.getId(), title, questionsList.size(), totalScore);
            
            return savedPaper;
        } catch (Exception e) {
            logger.error("创建试卷失败: ", e);
            throw new RuntimeException("创建试卷失败: " + e.getMessage());
        }
    }
    
    @Override
    public Long countQuestionsByPaperId(Long paperId) {
        return paperQuestionRepository.countByPaperId(paperId);
    }
    
    @Override
    public Integer sumScoreByPaperId(Long paperId) {
        return paperQuestionRepository.sumScoreByPaperId(paperId);
    }
    
    @Override
    public byte[] exportToWord(Long paperId) throws Exception {
        Optional<Paper> paperOpt = findById(paperId);
        if (!paperOpt.isPresent()) {
            throw new RuntimeException("试卷不存在");
        }
        
        Paper paper = paperOpt.get();
        List<PaperQuestion> paperQuestions = getPaperQuestions(paperId);
        
        // 创建Word文档
        XWPFDocument document = new XWPFDocument();
        
        try {
            // 设置页面边距
            document.getDocument().getBody().addNewSectPr().addNewPgMar();
            
            // 标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(paper.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            titleRun.setFontFamily("宋体");
            
            // 试卷信息
            XWPFParagraph infoParagraph = document.createParagraph();
            infoParagraph.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun infoRun = infoParagraph.createRun();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
            String infoText = String.format("科目：%s    总分：%d分    时长：%d分钟    创建时间：%s", 
                                           paper.getSubject(), 
                                           paper.getTotalScore(), 
                                           paper.getDuration(),
                                           sdf.format(paper.getCreatedAt()));
            infoRun.setText(infoText);
            infoRun.setFontSize(12);
            infoRun.setFontFamily("宋体");
            
            // 分隔线
            XWPFParagraph separatorParagraph = document.createParagraph();
            XWPFRun separatorRun = separatorParagraph.createRun();
            separatorRun.setText("————————————————————————————————————————————————");
            separatorRun.setFontSize(10);
            
            // 题目内容
            for (int i = 0; i < paperQuestions.size(); i++) {
                PaperQuestion pq = paperQuestions.get(i);
                Question question = pq.getQuestion();
                
                // 题目标题
                XWPFParagraph questionParagraph = document.createParagraph();
                XWPFRun questionRun = questionParagraph.createRun();
                questionRun.setText(String.format("%d. %s (%d分)", i + 1, question.getContent(), pq.getScore()));
                questionRun.setBold(true);
                questionRun.setFontSize(12);
                questionRun.setFontFamily("宋体");
                
                // 选项（如果有）
                if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                    for (int j = 0; j < question.getOptions().size(); j++) {
                        XWPFParagraph optionParagraph = document.createParagraph();
                        XWPFRun optionRun = optionParagraph.createRun();
                        optionRun.setText(String.format("   %s. %s", 
                                        (char)('A' + j), question.getOptions().get(j)));
                        optionRun.setFontSize(11);
                        optionRun.setFontFamily("宋体");
                    }
                }
                
                // 空行
                document.createParagraph();
            }
            
            // 答案部分
            XWPFParagraph answerTitleParagraph = document.createParagraph();
            answerTitleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun answerTitleRun = answerTitleParagraph.createRun();
            answerTitleRun.setText("参考答案");
            answerTitleRun.setBold(true);
            answerTitleRun.setFontSize(14);
            answerTitleRun.setFontFamily("宋体");
            
            // 答案内容
            for (int i = 0; i < paperQuestions.size(); i++) {
                PaperQuestion pq = paperQuestions.get(i);
                Question question = pq.getQuestion();
                
                XWPFParagraph answerParagraph = document.createParagraph();
                XWPFRun answerRun = answerParagraph.createRun();
                answerRun.setText(String.format("%d. %s", i + 1, question.getAnswer()));
                answerRun.setFontSize(11);
                answerRun.setFontFamily("宋体");
                
                // 解析（如果有）
                if (question.getAnalysis() != null && !question.getAnalysis().trim().isEmpty()) {
                    XWPFParagraph analysisParagraph = document.createParagraph();
                    XWPFRun analysisRun = analysisParagraph.createRun();
                    analysisRun.setText("   解析：" + question.getAnalysis());
                    analysisRun.setFontSize(10);
                    analysisRun.setFontFamily("宋体");
                    analysisRun.setColor("666666");
                }
            }
            
            // 转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();
            
        } finally {
            document.close();
        }
    }
}
