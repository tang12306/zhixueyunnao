package com._1.service.impl;

import com._1.entity.*;
import com._1.repository.ClassEntityRepository;
import com._1.repository.ExamRepository;
import com._1.repository.ScoreRepository;
import com._1.repository.UserRepository;
import com._1.service.ScoreService;
import com._1.service.UserService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScoreServiceImpl implements ScoreService {

    private static final Logger logger = LoggerFactory.getLogger(ScoreServiceImpl.class);

    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final ClassEntityRepository classEntityRepository;
    private final UserService userService;

    public ScoreServiceImpl(ScoreRepository scoreRepository,
                            UserRepository userRepository,
                            ExamRepository examRepository,
                            ClassEntityRepository classEntityRepository,
                            UserService userService) {
        this.scoreRepository = scoreRepository;
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.classEntityRepository = classEntityRepository;
        this.userService = userService;
    }

    @Override
    public List<Score> findAll() {
        return scoreRepository.findAll();
    }
    
    @Override
    public Page<Score> findAll(Pageable pageable) {
        return scoreRepository.findAll(pageable);
    }

    @Override
    public Optional<Score> findById(Long id) {
        return scoreRepository.findById(id);
    }

    @Override
    public List<Score> findByUser(User user) {
        return scoreRepository.findByUser(user);
    }

    @Override
    public List<Score> findByExam(Exam exam) {
        return scoreRepository.findByExam(exam);
    }
    
    @Override
    public Optional<Score> findByUserAndExam(User user, Exam exam) {
        return scoreRepository.findByUserAndExam(user, exam);
    }
    
    @Override
    public List<Score> findByClassAndExam(ClassEntity classEntity, Exam exam) {
        return scoreRepository.findByClassAndExam(classEntity, exam);
    }

    @Override
    public Score save(Score score) {
        return scoreRepository.save(score);
    }

    @Override
    public void deleteById(Long id) {
        scoreRepository.deleteById(id);
    }
    
    @Override
    public Double findAverageScoreByExamAndClass(Exam exam, ClassEntity classEntity) {
        return scoreRepository.findAverageScoreByExamAndClass(exam, classEntity);
    }
    
    @Override
    public Double findMaxScoreByExamAndClass(Exam exam, ClassEntity classEntity) {
        return scoreRepository.findMaxScoreByExamAndClass(exam, classEntity);
    }
    
    @Override
    public Double findMinScoreByExamAndClass(Exam exam, ClassEntity classEntity) {
        return scoreRepository.findMinScoreByExamAndClass(exam, classEntity);
    }
    
    @Override
    @Transactional
    public List<Score> importScores(MultipartFile file, Long examId) {
        List<Score> importedScores = new ArrayList<>();
        
        try (InputStream is = file.getInputStream()) {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            
            Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("考试不存在: " + examId));
            
            // 跳过表头行
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                String studentUsername = getCellValueAsString(row.getCell(0));
                Double scoreValue = getCellValueAsDouble(row.getCell(2));
                
                if (studentUsername != null && scoreValue != null) {
                    // 查找学生
                    Optional<User> userOpt = userRepository.findByUsername(studentUsername);
                    
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        
                        // 检查是否已存在该学生的成绩记录
                        Optional<Score> existingScore = scoreRepository.findByUserAndExam(user, exam);
                        
                        Score score;
                        if (existingScore.isPresent()) {
                            score = existingScore.get();
                            score.setScore(scoreValue);
                        } else {
                            score = new Score();
                            score.setUser(user);
                            score.setExam(exam);
                            score.setScore(scoreValue);
                            score.setCreateTime(new Date());
                        }
                        
                        importedScores.add(scoreRepository.save(score));
                    } else {
                        // 学生不存在，创建一个临时成绩记录
                        Score score = new Score();
                        score.setExam(exam);
                        score.setScore(scoreValue);
                        score.setCreateTime(new Date());
                        importedScores.add(scoreRepository.save(score));
                    }
                }
            }
            
            workbook.close();
        } catch (IOException e) {
            logger.error("IOException during score import for exam ID {}: {}", examId, e.getMessage(), e);
            throw new RuntimeException("导入Excel成绩失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected exception during score import for exam ID {}: {}", examId, e.getMessage(), e);
            throw new RuntimeException("导入成绩时发生未知错误: " + e.getMessage(), e);
        }
        
        return importedScores;
    }
    
    @Override
    public Map<String, Object> analyzeScoresByExam(Long examId) {
        Map<String, Object> result = new HashMap<>();
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("考试不存在: " + examId));
        
        List<Score> scores = scoreRepository.findByExam(exam);
        
        if (scores.isEmpty()) {
            result.put("message", "该考试尚无成绩记录。");
            return result;
        }
        
        DoubleSummaryStatistics stats = scores.stream()
            .filter(s -> s.getScore() != null)
            .mapToDouble(Score::getScore)
            .summaryStatistics();
        
        result.put("examName", exam.getName());
        result.put("totalStudents", stats.getCount());
        result.put("averageScore", stats.getAverage());
        result.put("maxScore", stats.getMax());
        result.put("minScore", stats.getMin());
        
        Map<String, Long> distribution = new HashMap<>();
        distribution.put("优秀(90-100)", scores.stream().filter(s -> s.getScore() != null && s.getScore() >= 90).count());
        distribution.put("良好(80-89)", scores.stream().filter(s -> s.getScore() != null && s.getScore() >= 80 && s.getScore() < 90).count());
        distribution.put("中等(70-79)", scores.stream().filter(s -> s.getScore() != null && s.getScore() >= 70 && s.getScore() < 80).count());
        distribution.put("及格(60-69)", scores.stream().filter(s -> s.getScore() != null && s.getScore() >= 60 && s.getScore() < 70).count());
        distribution.put("不及格(<60)", scores.stream().filter(s -> s.getScore() != null && s.getScore() < 60).count());
        
        result.put("distribution", distribution);
        
        return result;
    }
    
    @Override
    public Map<String, Object> analyzeStudentScoresTrend(Long userId, Long subjectId) {
        Map<String, Object> result = new HashMap<>();
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));
        
        List<Score> allScores = scoreRepository.findByUser(user);
        
        List<Score> subjectScores = allScores.stream()
            .filter(score -> score.getExam() != null && score.getExam().getSubject() != null && score.getExam().getSubject().getId().equals(subjectId))
            .sorted(Comparator.comparing(s -> s.getExam().getExamDate(), Comparator.nullsLast(Date::compareTo)))
            .collect(Collectors.toList());
        
        if (subjectScores.isEmpty()) {
            result.put("message", "该学生在此科目下尚无成绩记录。");
            return result;
        }
        
        List<String> examNames = new ArrayList<>();
        List<Double> scoresData = new ArrayList<>();
        List<Integer> ranks = new ArrayList<>();
        
        for (Score score : subjectScores) {
            examNames.add(score.getExam().getName());
            scoresData.add(score.getScore());
            ranks.add(score.getRank() != null ? score.getRank() : 0);
        }
        
        result.put("studentName", user.getName());
        result.put("studentUsername", user.getUsername());
        result.put("examNames", examNames);
        result.put("scores", scoresData);
        result.put("ranks", ranks);
        
        return result;
    }
    
    @Override
    public Map<String, Object> compareClassesAverageScores(List<Long> classIds, Long examId) {
        Map<String, Object> result = new HashMap<>();
        
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("考试不存在: " + examId));
        
        Map<String, Double> classAverages = new HashMap<>();
        
        for (Long classId : classIds) {
            Optional<ClassEntity> classEntityOpt = classEntityRepository.findById(classId);
            if (classEntityOpt.isPresent()) {
                ClassEntity classEntity = classEntityOpt.get();
                Double avgScore = scoreRepository.findAverageScoreByExamAndClass(exam, classEntity);
                
                if (avgScore != null) {
                    classAverages.put(classEntity.getName(), avgScore);
                } else {
                    classAverages.put(classEntity.getName(), 0.0);
                }
            }
        }
        
        result.put("examName", exam.getName());
        result.put("classAverages", classAverages);
        
        return result;
    }
    
    @Override
    @Transactional
    public List<Score> importScoresFromExcel(MultipartFile file, Exam exam, ClassEntity classEntity) throws IOException {
        List<Score> importedScores = new ArrayList<>();
        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) { // Skip header
                rowIterator.next();
            }

            List<User> classUsers = userRepository.findByStudentClassIdAndRole(classEntity.getId(), "STUDENT");
            Map<String, User> userMapByUsername = classUsers.stream()
                .collect(Collectors.toMap(User::getUsername, u -> u));

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String studentUsername = getCellValueAsString(row.getCell(0));
                Double scoreValue = getCellValueAsDouble(row.getCell(2));

                if (studentUsername != null && !studentUsername.trim().isEmpty() && scoreValue != null) {
                    User user = userMapByUsername.get(studentUsername.trim());

                    if (user != null) {
                        Optional<Score> existingScoreOpt = scoreRepository.findByUserAndExam(user, exam);
                        Score scoreToSave;
                        if (existingScoreOpt.isPresent()) {
                            scoreToSave = existingScoreOpt.get();
                            scoreToSave.setScore(scoreValue);
                        } else {
                            scoreToSave = new Score();
                            scoreToSave.setUser(user);
                            scoreToSave.setExam(exam);
                            scoreToSave.setScore(scoreValue);
                            scoreToSave.setCreateTime(new Date());
                        }
                        importedScores.add(scoreRepository.save(scoreToSave));
                    } else {
                        logger.warn("Student with username '{}' not found in class '{}' or is not a student, during score import for exam '{}'. Score not saved for this entry.",
                                    studentUsername, classEntity.getName(), exam.getName());
                    }
                } else if (studentUsername != null && !studentUsername.trim().isEmpty() && scoreValue == null) {
                     logger.warn("Score value is null for student username '{}' in class '{}', exam '{}'. Entry skipped.",
                                    studentUsername, classEntity.getName(), exam.getName());
                }
            }
            workbook.close();
            
            // Calculate ranks after all scores for this exam and class are imported/updated
            if (!importedScores.isEmpty()) {
                List<Score> allScoresForExamInClass = scoreRepository.findByClassAndExam(classEntity, exam);
                if(allScoresForExamInClass != null && !allScoresForExamInClass.isEmpty()){
                    calculateRanks(allScoresForExamInClass);
                }
            }

        } catch (IOException e) {
            logger.error("IOException during Excel score import for exam '{}', class '{}': {}", exam.getName(), classEntity.getName(), e.getMessage(), e);
            throw new IOException("导入Excel成绩失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected exception during Excel score import for exam '{}', class '{}': {}", exam.getName(), classEntity.getName(), e.getMessage(), e);
            throw new RuntimeException("导入成绩时发生未知错误: " + e.getMessage(), e);
        }
        return importedScores;
    }
    
    @Override
    public Double getClassAverageScore(Long classId, Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));
        ClassEntity classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found with ID: " + classId));
        return scoreRepository.findAverageScoreByExamAndClass(exam, classEntity);
    }
    
    @Override
    public Map<String, Integer> getClassScoreDistribution(Long classId, Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));
        ClassEntity classEntity = classEntityRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found with ID: " + classId));

        List<Score> scoresInClass = scoreRepository.findByClassAndExam(classEntity, exam);
        Map<String, Integer> distribution = new TreeMap<>();
        distribution.put("90-100 (优秀)", 0);
        distribution.put("80-89 (良好)", 0);
        distribution.put("70-79 (中等)", 0);
        distribution.put("60-69 (及格)", 0);
        distribution.put("<60 (不及格)", 0);

        if (scoresInClass != null) {
            for (Score score : scoresInClass) {
                if (score.getScore() != null) {
                    double s = score.getScore();
                    if (s >= 90) distribution.merge("90-100 (优秀)", 1, Integer::sum);
                    else if (s >= 80) distribution.merge("80-89 (良好)", 1, Integer::sum);
                    else if (s >= 70) distribution.merge("70-79 (中等)", 1, Integer::sum);
                    else if (s >= 60) distribution.merge("60-69 (及格)", 1, Integer::sum);
                    else distribution.merge("<60 (不及格)", 1, Integer::sum);
                }
            }
        }
        return distribution;
    }
    
    // Helper method to calculate ranks within a list of scores
    private void calculateRanks(List<Score> scores) {
        if (scores == null || scores.isEmpty()) {
            return;
        }
        // Sort scores in descending order. Handle null scores by placing them at the end.
        scores.sort((s1, s2) -> {
            if (s1.getScore() == null && s2.getScore() == null) return 0;
            if (s1.getScore() == null) return 1; // s1 is null, s2 is not, s1 is "greater" (comes after)
            if (s2.getScore() == null) return -1; // s2 is null, s1 is not, s1 is "lesser" (comes before)
            return s2.getScore().compareTo(s1.getScore()); // Descending order
        });

        int rank = 1;
        for (int i = 0; i < scores.size(); i++) {
            Score currentScore = scores.get(i);
            if (currentScore.getScore() == null) { // Scores with null value get no rank or last rank
                 currentScore.setRank(null); // Or some indicator for unranked
                 continue;
            }
            if (i > 0 && scores.get(i-1).getScore() != null && currentScore.getScore().equals(scores.get(i - 1).getScore())) {
                // Same score as previous, same rank
                currentScore.setRank(scores.get(i - 1).getRank());
            } else {
                currentScore.setRank(rank);
            }
            rank++; // Increment rank for the next distinct score, or use i+1 for simple sequential rank
        }
        scoreRepository.saveAll(scores); // Save updated ranks
    }
    
    // Helper method to get cell value as String
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString(); // Or format as needed
                } else {
                    // Format as integer if it's a whole number, otherwise as double string
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // Try to evaluate formula cell as string
                try {
                    return cell.getStringCellValue().trim();
                } catch (IllegalStateException e) {
                    // If formula result is not a string, try numeric
                     try {
                        double numericValue = cell.getNumericCellValue();
                         if (numericValue == Math.floor(numericValue)) {
                            return String.valueOf((long) numericValue);
                        } else {
                            return String.valueOf(numericValue);
                        }
                    } catch (IllegalStateException ex) {
                        logger.warn("Cannot evaluate formula cell to string or numeric: {}", cell.getCellFormula());
                        return null; // Or some error indicator
                    }
                }
            default:
                return null;
        }
    }
    
    // Helper method to get cell value as Double
    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null; // Or handle error
                }
            case FORMULA:
                 try {
                    return cell.getNumericCellValue();
                } catch (IllegalStateException e) {
                    // If formula result is not numeric, try to parse from string if it's string type
                    try {
                        return Double.parseDouble(cell.getStringCellValue().trim());
                    } catch (Exception ex) {
                        logger.warn("Cannot evaluate formula cell to numeric or parse from string: {}", cell.getCellFormula());
                        return null;
                    }
                }
            default:
                return null;
        }
    }
} 
