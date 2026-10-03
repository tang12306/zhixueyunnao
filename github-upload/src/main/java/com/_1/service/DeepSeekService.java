package com._1.service;

import com._1.config.DeepSeekConfig;
import com._1.dto.QuestionAnswerDetail;
import com._1.entity.Exam;
import com._1.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com._1.entity.ReflectionReport;
import com._1.entity.Score;
import com._1.entity.Question;
import com._1.entity.QuestionType;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * DeepSeek服务，用于与AI模型进行交互
 * 注：这里使用模拟实现，实际项目中可以接入真实的AI服务
 */
@Service
public class DeepSeekService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekService.class);
    private final DeepSeekConfig config;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    
    public DeepSeekService(DeepSeekConfig config) {
        this.config = config;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
        
        // 测试网络连接
        testNetworkConnection();
    }
    
    private void testNetworkConnection() {
        try {
            logger.info("测试网络连接...");
            // 测试与DeepSeek API的连接
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("api.deepseek.com", 443), 5000);
                logger.info("成功连接到 api.deepseek.com:443");
            }
        } catch (IOException e) {
            logger.error("网络连接测试失败", e);
        }
    }
    
    public String generateQuestion(String prompt) throws IOException {
        logger.info("开始生成题目，提示词长度: {} 字符", prompt.length());
        logger.debug("提示词内容: {}", prompt);
        
        // 清理提示词，移除控制字符和正确转义JSON特殊字符
        String cleanedPrompt = cleanJsonString(prompt);

        String jsonBody = String.format("""
            {
                "model": "deepseek-chat",
                "messages": [
                    {
                        "role": "system",
                        "content": "你是一个专业的教师，擅长出题。请根据要求生成题目。生成的题目必须遵循JSON格式，不要添加额外的解释文字。"
                    },
                    {
                        "role": "user",
                        "content": "%s"
                    }
                ],
                "temperature": 0.7,
                "max_tokens": 4000,
                "stream": false
            }
            """, cleanedPrompt);
            
        logger.info("准备发送请求到: {}", config.getApiUrl());
        logger.debug("请求体: {}", jsonBody);
            
        Request request = new Request.Builder()
                .url(config.getApiUrl())
                .addHeader("Authorization", "Bearer " + config.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
                .build();
        
        // 创建一个带有更长超时的client
        OkHttpClient timeoutClient = client.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS) // 连接超时30秒
                .readTimeout(120, TimeUnit.SECONDS)   // 读取超时2分钟
                .writeTimeout(30, TimeUnit.SECONDS)   // 写入超时30秒
                .build();
                
        try (Response response = timeoutClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "未知错误";
                logger.error("API调用失败: {} - {}", response.code(), errorBody);
                throw new IOException("API调用失败: " + response.code() + ", 错误信息: " + errorBody);
            }
            
            String responseBody = response.body().string();
            logger.info("收到API响应，长度: {} 字符", responseBody.length());
            logger.debug("响应内容: {}", responseBody);
            
            try {
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                JsonNode choices = jsonNode.path("choices");
                if (choices.isEmpty() || !choices.isArray()) {
                    logger.error("API响应格式错误: choices 为空或不是数组");
                    throw new IOException("API响应格式错误: choices 为空或不是数组");
                }
                
                JsonNode firstChoice = choices.get(0);
                if (firstChoice == null) {
                    logger.error("API响应格式错误: 第一个 choice 为空");
                    throw new IOException("API响应格式错误: 第一个 choice 为空");
                }
                
                JsonNode message = firstChoice.path("message");
                if (message.isMissingNode()) {
                    logger.error("API响应格式错误: message 字段缺失");
                    throw new IOException("API响应格式错误: message 字段缺失");
                }
                
                JsonNode content = message.path("content");
                if (content.isMissingNode()) {
                    logger.error("API响应格式错误: content 字段缺失");
                    throw new IOException("API响应格式错误: content 字段缺失");
                }
                
                String result = content.asText();
                logger.info("成功解析API响应，返回内容长度: {} 字符", result.length());
                return result;
            } catch (Exception e) {
                logger.error("解析API响应失败: {}", e.getMessage(), e);
                logger.error("原始响应数据: {}", responseBody);
                throw new IOException("解析API响应失败", e);
            }
        } catch (IOException e) {
            logger.error("API调用过程中出现IO异常: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("API调用过程中出现未知异常: {}", e.getMessage(), e);
            throw new IOException("API调用过程中出现未知异常", e);
        }
    }

    /**
     * 根据考试成绩、用户、考试详情和答案详情调用 Deepseek AI 生成反思报告
     * @param currentUser 当前用户对象
     * @param exam 当前考试对象
     * @param savedScore 保存后的成绩对象
     * @param answerDetails 学生答案详情列表
     * @return 生成的 ReflectionReport 对象，如果失败则返回 null
     */
    public ReflectionReport generateReflectionReport(User currentUser, Exam exam, Score savedScore, List<QuestionAnswerDetail> answerDetails) {
        try {
            // Format answer details for the prompt
            String answerDetailsString = answerDetails.stream()
                .map(ad -> String.format("题目: %s (得分: %.1f), 你的答案: %s, 正确答案: %s, 是否正确: %s",
                                        ad.getQuestion() != null ? ad.getQuestion().getContent() : "N/A",
                                        ad.getQuestionScore(),
                                        ad.getStudentAnswer(),
                                        ad.getCorrectAnswer(),
                                        ad.isCorrect() ? "是" : "否"))
                .collect(Collectors.joining("\n"));

            // 组装提示词
            String prompt = String.format("请针对以下考试结果，生成薄弱项、错题分析、正确答案解析，并为每道错题生成一道相似题(含答案与解析)。\n"
                    + "学生姓名: %s (学号: %s)\n"
                    + "考试科目: %s\n"
                    + "考试名称: %s\n"
                    + "总得分: %.1f\n"
                    + "答题详情:\n%s\n", 
                    currentUser != null && currentUser.getName() != null ? currentUser.getName() : "N/A",
                    currentUser != null ? currentUser.getUsername() : "N/A",
                    exam != null && exam.getSubject() != null && exam.getSubject().getName() != null ? exam.getSubject().getName() : "N/A",
                    exam != null && exam.getName() != null ? exam.getName() : "未知考试",
                    savedScore != null && savedScore.getScore() != null ? savedScore.getScore() : 0.0,
                    answerDetailsString);
            
            logger.debug("DeepSeek Prompt for reflection: {}", prompt);

            String aiResult;
            try {
                aiResult = generateQuestion(prompt);
            } catch (Exception e) {
                logger.warn("调用 DeepSeek 生成反思报告失败, 将使用占位数据: {}", e.getMessage());
                aiResult = "AI报告生成失败，请稍后查看或联系管理员。错误: " + e.getMessage();
            }

            ReflectionReport report = new ReflectionReport();
            report.setScore(savedScore);
            report.setUser(currentUser);
            
            report.setWeaknessAnalysis(aiResult);
            report.setMistakeAnalysis("详情请见综合分析部分。后续可解析AI结果分别填充。");
            report.setCorrectAnswerAnalysis("详情请见综合分析部分。后续可解析AI结果分别填充。");
            report.setPracticeProblems("详情请见综合分析部分。后续可解析AI结果分别填充。");
            
            report.setGeneratedAt(new java.util.Date());
            
            logger.info("为用户 {} 的考试 {} 生成的反思报告内容初步设定完成。", 
                currentUser != null ? currentUser.getUsername() : "unknown_user", 
                exam != null ? exam.getName() : "unknown_exam");
            return report;
            
        } catch (Exception e) {
            logger.error("生成反思报告过程中发生意外错误 for user {} and exam {}: {}", 
                         (currentUser != null ? currentUser.getUsername() : "unknown_user"), 
                         (exam != null ? exam.getName() : "unknown_exam"), 
                         e.getMessage(), e);
            return null;
        }
    }

    /**
     * 生成AI内容
     */
    public String generateContent(String prompt) {
        logger.info("发送AI生成请求，提示词: {}", prompt);

        try {
            // 调用真实的DeepSeek API
            String result = generateQuestion(prompt);
            logger.info("成功从DeepSeek API获取响应，长度为: {} 字符", result.length());
            return result;
        } catch (Exception e) {
            logger.error("调用AI生成内容失败: {} - {}", e.getClass().getName(), e.getMessage(), e);

            // 详细记录错误信息
            if (e.getCause() != null) {
                logger.error("根本原因: {}", e.getCause().getMessage());
            }

            // 检查API密钥配置
            if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
                logger.error("DeepSeek API密钥未配置或为空");
                throw new RuntimeException("DeepSeek API密钥未配置，请检查配置文件");
            }

            // 检查API URL配置
            if (config.getApiUrl() == null || config.getApiUrl().trim().isEmpty()) {
                logger.error("DeepSeek API URL未配置或为空");
                throw new RuntimeException("DeepSeek API URL未配置，请检查配置文件");
            }

            // 尝试诊断连接问题
            try {
                logger.info("尝试测试与API服务器的连接...");
                testNetworkConnection();
            } catch (Exception connectionError) {
                logger.error("与API服务器连接测试也失败: {}", connectionError.getMessage());
            }

            // 重新抛出异常，不再返回模拟数据
            throw new RuntimeException("DeepSeek API调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析AI生成的JSON，提取问题列表
     */
    public List<Question> parseGeneratedQuestionsFromJson(String jsonResponse) {
        List<Question> questions = new ArrayList<>();
        
        try {
            // 从JSON字符串中提取```json ```包裹的内容
            String jsonContent = extractJsonFromResponse(jsonResponse);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                logger.warn("无法从响应中提取JSON内容");
                return questions;
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            JsonNode questionsNode = rootNode.get("questions");
            
            if (questionsNode != null && questionsNode.isArray()) {
                for (JsonNode questionNode : questionsNode) {
                    Question question = new Question();
                    question.setContent(questionNode.get("content").asText());
                    
                    // 将字符串类型转换为QuestionType枚举
                    String typeStr = questionNode.get("type").asText();
                    QuestionType questionType;
                    switch(typeStr) {
                        case "SINGLE_CHOICE":
                            questionType = QuestionType.SINGLE_CHOICE;
                            break;
                        case "MULTIPLE_CHOICE":
                            questionType = QuestionType.MULTIPLE_CHOICE;
                            break;
                        case "FILL_BLANK":
                        case "FILL_IN_THE_BLANK":
                            questionType = QuestionType.FILL_IN_THE_BLANK;
                            break;
                        case "SHORT_ANSWER":
                            questionType = QuestionType.SHORT_ANSWER;
                            break;
                        default:
                            questionType = QuestionType.SHORT_ANSWER; // 默认值
                    }
                    question.setType(questionType);
                    question.setScore(questionNode.get("score").asInt());
                    
                    if (questionNode.has("answer")) {
                        question.setAnswer(questionNode.get("answer").asText());
                    }
                    
                    if (questionNode.has("analysis")) {
                        question.setAnalysis(questionNode.get("analysis").asText());
                    }
                    
                    // 设置默认科目，避免空值
                    question.setSubject("未分类");
                    
                    // 设置默认难度
                    question.setDifficulty(2); // 中等难度
                    
                    // 处理选项 - 从Option对象转换为字符串列表
                    if (questionNode.has("options") && questionNode.get("options").isArray()) {
                        List<String> optionStrings = new ArrayList<>();
                        for (JsonNode optionNode : questionNode.get("options")) {
                            String key = optionNode.get("key").asText();
                            String content = optionNode.get("content").asText();
                            optionStrings.add(key + ". " + content);
                        }
                        question.setOptions(optionStrings);
                    }
                    
                    questions.add(question);
                }
            }
        } catch (Exception e) {
            logger.error("解析AI生成的JSON失败", e);
        }
        
        return questions;
    }

    /**
     * 清理JSON字符串，移除控制字符并正确转义特殊字符
     */
    private String cleanJsonString(String input) {
        if (input == null) {
            return "";
        }

        // 移除控制字符 (U+0000 到 U+001F)
        String cleaned = input.replaceAll("[\\u0000-\\u001F]", "");

        // 转义JSON特殊字符
        cleaned = cleaned.replace("\\", "\\\\")  // 反斜杠
                        .replace("\"", "\\\"")   // 双引号
                        .replace("\b", "\\b")    // 退格
                        .replace("\f", "\\f")    // 换页
                        .replace("\n", "\\n")    // 换行
                        .replace("\r", "\\r")    // 回车
                        .replace("\t", "\\t");   // 制表符

        return cleaned;
    }

    /**
     * 从响应中提取JSON内容
     */
    private String extractJsonFromResponse(String response) {
        try {
            int startIndex = response.indexOf("```json");
            int endIndex = response.lastIndexOf("```");

            if (startIndex >= 0 && endIndex > startIndex) {
                return response.substring(startIndex + 7, endIndex).trim();
            }

            // 尝试直接解析整个响应
            objectMapper.readTree(response);
            return response;
        } catch (Exception e) {
            logger.error("提取JSON失败", e);
            return null;
        }
    }

    /**
     * 模拟选择题响应
     */
    private String mockChoiceQuestionsResponse() {
        return """
            ```json
            {
              "questions": [
                {
                  "type": "SINGLE_CHOICE",
                  "content": "若函数f(x)=x²在点x=1处的导数为：",
                  "score": 3,
                  "options": [
                    { "key": "A", "content": "0" },
                    { "key": "B", "content": "1" },
                    { "key": "C", "content": "2" },
                    { "key": "D", "content": "3" }
                  ],
                  "answer": "C",
                  "analysis": "函数f(x)=x²的导数是f'(x)=2x，所以f'(1)=2。"
                },
                {
                  "type": "SINGLE_CHOICE",
                  "content": "下列函数中，在x=0处连续的是：",
                  "score": 3,
                  "options": [
                    { "key": "A", "content": "f(x)=sin(1/x)" },
                    { "key": "B", "content": "f(x)=|x|/x" },
                    { "key": "C", "content": "f(x)=x·sin(1/x)" },
                    { "key": "D", "content": "f(x)=1/x" }
                  ],
                  "answer": "C",
                  "analysis": "A选项在x=0处无定义；B选项在x=0处无定义；C选项在x=0处的极限是0，且f(0)=0，所以连续；D选项在x=0处无定义。"
                },
                {
                  "type": "MULTIPLE_CHOICE",
                  "content": "以下哪些是初等函数？",
                  "score": 4,
                  "options": [
                    { "key": "A", "content": "幂函数" },
                    { "key": "B", "content": "指数函数" },
                    { "key": "C", "content": "对数函数" },
                    { "key": "D", "content": "Gamma函数" }
                  ],
                  "answer": "[A,B,C]",
                  "analysis": "初等函数包括幂函数、指数函数、对数函数、三角函数和反三角函数，而Gamma函数不是初等函数。"
                }
              ]
            }
            ```
        """;
    }

    /**
     * 模拟混合题型响应
     */
    private String mockMixedQuestionsResponse() {
        return """
            ```json
            {
              "questions": [
                {
                  "type": "SINGLE_CHOICE",
                  "content": "若函数f(x)=x²在点x=1处的导数为：",
                  "score": 3,
                  "options": [
                    { "key": "A", "content": "0" },
                    { "key": "B", "content": "1" },
                    { "key": "C", "content": "2" },
                    { "key": "D", "content": "3" }
                  ],
                  "answer": "C",
                  "analysis": "函数f(x)=x²的导数是f'(x)=2x，所以f'(1)=2。"
                },
                {
                  "type": "MULTIPLE_CHOICE",
                  "content": "以下哪些是初等函数？",
                  "score": 4,
                  "options": [
                    { "key": "A", "content": "幂函数" },
                    { "key": "B", "content": "指数函数" },
                    { "key": "C", "content": "对数函数" },
                    { "key": "D", "content": "Gamma函数" }
                  ],
                  "answer": "[A,B,C]",
                  "analysis": "初等函数包括幂函数、指数函数、对数函数、三角函数和反三角函数，而Gamma函数不是初等函数。"
                },
                {
                  "type": "FILL_BLANK",
                  "content": "函数f(x)=sin(x)在x=0处的导数是_______。",
                  "score": 3,
                  "answer": "1",
                  "analysis": "函数f(x)=sin(x)的导数是f'(x)=cos(x)，所以f'(0)=cos(0)=1。"
                },
                {
                  "type": "SHORT_ANSWER",
                  "content": "简述微积分基本定理的内容及其意义。",
                  "score": 10,
                  "answer": "微积分基本定理包含两部分：第一部分表明，如果f是连续函数，那么f的不定积分是f的原函数；第二部分表明，如果F是f的一个原函数，那么区间[a,b]上f的定积分等于F(b)-F(a)。该定理建立了微分和积分之间的联系，是微积分学中最重要的定理之一。",
                  "analysis": "微积分基本定理揭示了微分和积分这两种看似不同的运算之间的本质联系，为解决许多数学和物理问题提供了强大工具。"
                }
              ]
            }
            ```
        """;
    }
} 