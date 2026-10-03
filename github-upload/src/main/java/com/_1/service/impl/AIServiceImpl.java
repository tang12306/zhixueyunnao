package com._1.service.impl;

import com._1.service.AIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AIServiceImpl implements AIService {
    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.url}")
    private String deepseekApiUrl;

    @Value("${deepseek.api.key}")
    private String deepseekApiKey;

    @Autowired
    public AIServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public List<Map<String, Object>> generateQuestions(String subjectName, 
                                                     String chapterName,
                                                     String chapterDescription,
                                                     String type, 
                                                     Integer difficulty, List<String> tags, 
                                                     String topic, Integer count) {
        try {
            logger.info("开始生成题目 - 学科: {}, 章节: {} (描述: {}), 类型: {}, 难度: {}, 主题: {}, 数量: {}", 
                       subjectName, chapterName, chapterDescription, type, difficulty, topic, count);
            
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append(String.format("请为我生成%d道关于《%s》学科的%s题目。", count, subjectName, type));
            
            if (chapterName != null && !chapterName.isEmpty()) {
                prompt.append(String.format("题目应侧重于章节《%s》", chapterName));
                if (chapterDescription != null && !chapterDescription.isEmpty()) {
                    prompt.append(String.format("（该章节主要内容或学习目标是：%s）。", chapterDescription));
                } else {
                    prompt.append("。");
                }
            }

            if (difficulty != null) {
                prompt.append(String.format("难度级别请设定为%d级 (1-5级)，", difficulty));
            }
            
            if (topic != null && !topic.isEmpty()) {
                prompt.append(String.format("具体主题或知识点是\"%s\"，", topic));
            }
            
            if (tags != null && !tags.isEmpty()) {
                prompt.append(String.format("请包含以下标签: %s，", String.join(", ", tags)));
            }

            prompt.append("""
                请按照以下JSON格式返回题目，不要有任何其他文字或解释：
                [
                  {
                    "content": "题目内容",
                    "options": ["选项A", "选项B", "选项C", "选项D"], // 仅选择题需要
                    "answer": "正确答案",
                    "analysis": "解析和解答思路"
                  }
                ]""");

            logger.debug("生成的提示词: {}", prompt);

            // 调用 DeepSeek API
            String response = callDeepSeekAPI(prompt.toString());
            logger.debug("AI响应: {}", response);
            
            // 解析响应
            List<Map<String, Object>> questions = parseAIResponse(response);
            logger.info("成功生成{}道题目", questions.size());

            // 确保每个题目都有正确的类型信息
            questions.forEach(q -> {
                // 如果AI没有返回type字段或type为空，使用请求中的type
                if (!q.containsKey("type") || q.get("type") == null || q.get("type").toString().trim().isEmpty()) {
                    q.put("type", type);
                    logger.debug("为题目设置类型: {}", type);
                }
                // 确保score字段存在
                q.putIfAbsent("score", 5);
            });

            return questions;
        } catch (Exception e) {
            logger.error("生成题目失败 - 学科: {}, 章节: {} (描述: {}), 类型: {}, 难度: {}, 错误: {}", 
                         subjectName, chapterName, chapterDescription, type, difficulty, e.getMessage(), e);
            throw new RuntimeException("AI服务暂时不可用，请稍后再试", e);
        }
    }

    @Override
    public Map<String, Object> improveQuestion(Long questionId, String instruction) {
        // TODO: 实现题目优化功能
        throw new UnsupportedOperationException("题目优化功能尚未实现");
    }

    @Override
    public List<Map<String, Object>> generateQuestionsWithCustomPromptAndScore(
            String subjectName,
            List<String> chapterNames,
            List<String> chapterDescriptions,
            String type,
            Integer difficulty,
            Integer count,
            String customPrompt) {
        try {
            logger.info("开始批量生成题目 - 学科: {}, 章节数: {}, 类型: {}, 难度: {}, 数量: {}, 自定义提示: {}", 
                       subjectName, chapterNames != null ? chapterNames.size() : 0, type, difficulty, count, customPrompt != null && !customPrompt.isEmpty());
            
            StringBuilder prompt = new StringBuilder();
            prompt.append(String.format("请为我生成%d道关于《%s》学科的%s题目。", count, subjectName, type));

            if (chapterNames != null && !chapterNames.isEmpty()) {
                prompt.append("题目应重点考察以下章节内容：");
                for (int i = 0; i < chapterNames.size(); i++) {
                    prompt.append(String.format("《%s》", chapterNames.get(i)));
                    if (chapterDescriptions != null && i < chapterDescriptions.size() && chapterDescriptions.get(i) != null && !chapterDescriptions.get(i).isEmpty()) {
                        prompt.append(String.format("（该章节描述：%s）", chapterDescriptions.get(i)));
                    }
                    if (i < chapterNames.size() - 1) {
                        prompt.append("，");
                    }
                }
                prompt.append("。");
            }

            if (difficulty != null) {
                prompt.append(String.format("难度级别请设定为%d级 (1-5级)。", difficulty));
            }
            // 移除了原有对 topic 和 tags 的直接处理，假设这些可以通过 customPrompt 传入

            if (customPrompt != null && !customPrompt.trim().isEmpty()) {
                prompt.append("请特别注意以下出题要求：")
                      .append(customPrompt)
                      .append("。");
            }

            prompt.append("\n请为每道题目设定一个建议的分数（整数），例如5分或10分。");
            prompt.append("""
                请严格按照以下JSON格式返回题目列表，不要包含任何其他说明性文字或解释。确保JSON有效：
                [
                  {
                    "content": "题目内容",
                    "options": ["选项A", "选项B", "选项C", "选项D"], // 仅选择题需要此字段
                    "answer": "正确答案", // 对于多选题，答案可以是数组形式的字符串，如 [\"A\",\"B\"]，或单个字符串如 \"A,B\"
                    "analysis": "解析和解答思路",
                    "score": 10 // AI建议的题目分数 (整数)
                  }
                  // ... more questions
                ]""");

            logger.debug("生成给AI的Prompt: {}", prompt.toString());

            String response = callDeepSeekAPI(prompt.toString());
            logger.debug("AI响应内容: {}", response);
            
            List<Map<String, Object>> questions = parseAIResponse(response);
            logger.info("成功从AI处获得 {} 道题目（后续将进行校验和处理）", questions.size());

            // 确保每个题目都有正确的类型信息
            questions.forEach(q -> {
                // 如果AI没有返回type字段或type为空，使用请求中的type
                if (!q.containsKey("type") || q.get("type") == null || q.get("type").toString().trim().isEmpty()) {
                    q.put("type", type);
                    logger.debug("为题目设置类型: {}", type);
                }
                // 确保score字段存在
                q.putIfAbsent("score", 5);
            });

            return questions;
        } catch (Exception e) {
            logger.error("AI批量生成题目失败 - 学科: {}, 错误: {}", subjectName, e.getMessage(), e);
            // 将原始异常向上层抛出，由Controller统一处理API响应
            throw new RuntimeException("AI服务在生成题目时遇到问题: " + e.getMessage(), e);
        }
    }

    private String callDeepSeekAPI(String prompt) {
        try {
            logger.debug("准备调用DeepSeek API - URL: {}", deepseekApiUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepseekApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "你是一个专业的教育题目生成助手，擅长创建高质量的教育题目。"),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            logger.debug("请求体: {}", objectMapper.writeValueAsString(requestBody));

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(deepseekApiUrl, request, Map.class);
            Map<String, Object> response = responseEntity.getBody();
            
            logger.debug("API响应状态码: {}", responseEntity.getStatusCode());
            logger.debug("API响应体: {}", objectMapper.writeValueAsString(response));

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    return message.get("content");
                }
            }
            throw new RuntimeException("AI响应格式不正确: " + objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            logger.error("调用DeepSeek API失败 - URL: {}, 错误: {}", deepseekApiUrl, e.getMessage(), e);
            throw new RuntimeException("AI服务暂时不可用，请稍后再试", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAIResponse(String response) {
        try {
            logger.debug("开始解析AI响应: {}", response);
            
            // 提取JSON部分
            int startIndex = response.indexOf('[');
            int endIndex = response.lastIndexOf(']') + 1;
            
            if (startIndex == -1 || endIndex == 0) {
                throw new RuntimeException("响应中未找到JSON数组");
            }
            
            String jsonStr = response.substring(startIndex, endIndex);
            logger.debug("提取的JSON字符串: {}", jsonStr);
            
            List<Map<String, Object>> questions = objectMapper.readValue(jsonStr, List.class);
            logger.debug("解析后的题目数量: {}", questions.size());
            
            return questions;
        } catch (Exception e) {
            logger.error("解析AI响应失败 - 响应内容: {}, 错误: {}", response, e.getMessage(), e);
            throw new RuntimeException("AI生成的题目格式不正确", e);
        }
    }
} 