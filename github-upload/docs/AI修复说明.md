# AI功能修复说明

## 修复的问题

### 1. AI一键出卷不会真正向DeepSeek发送请求的问题

**问题原因：**
- 在`DeepSeekService.generateContent()`方法中，当API调用失败时会返回模拟数据
- 这导致用户以为系统在正常工作，但实际上没有调用真实的AI服务

**修复方案：**
1. 修改`DeepSeekService.generateContent()`方法，移除模拟数据返回逻辑
2. 增加详细的错误日志和配置检查
3. 当API调用失败时，直接抛出异常而不是返回模拟数据

**修复代码位置：**
- `教务题库系统/src/main/java/com/_1/service/DeepSeekService.java` 第232-274行

**修复内容：**
```java
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
```

### 2. AI一键出题保存时不会自动给出题目类型的问题

**问题原因：**
1. AI生成的题目可能没有包含类型信息
2. 前端在编辑题目时，类型字段可能被清空或修改
3. 保存时没有确保题目类型的正确传递

**修复方案：**
1. 在AI服务中确保生成的题目包含正确的类型信息
2. 在前端确保题目类型正确设置和传递
3. 改进前端UI，使题目类型更易于编辑

**修复代码位置：**

#### 后端修复：
- `教务题库系统/src/main/java/com/_1/service/impl/AIServiceImpl.java` 第89-104行和第166-180行

**修复内容：**
```java
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
```

#### 前端修复：
- `教务题库系统/frontend/src/views/ai-quiz/AICreateQuestion.vue` 第228-238行

**修复内容：**
```javascript
generatedQuestions.value = response.questions.map(q => ({
    ...q,
    tempId: uuidv4(),
    originalScore: q.score,
    score: q.score || 5,
    // 确保题目类型正确设置，如果AI没有返回类型，使用表单中选择的类型
    type: q.type || form.type,
    optionsString: Array.isArray(q.options) ? q.options.join('\n') : '',
}));
```

- `教务题库系统/frontend/src/views/ai-quiz/AICreateQuestion.vue` 第273-284行

**修复内容：**
```javascript
const questionsToSave = generatedQuestions.value.map(q => ({
    subjectId: form.subjectId,
    chapterId: q.chapterId,
    type: q.type || form.type, // 确保题目类型不为空
    difficulty: q.difficulty || form.difficulty,
    content: q.content,
    options: q.options,
    answer: q.answer,
    analysis: q.analysis,
    score: q.score || 5,
}));
```

- `教务题库系统/frontend/src/views/ai-quiz/AICreateQuestion.vue` 第107-116行

**UI改进：**
将题目类型从文本输入框改为下拉选择框：
```vue
<el-form-item label="题型">
  <el-select v-model="question.type" placeholder="选择题型" style="width: 100%;">
    <el-option label="单选题" value="单选题"></el-option>
    <el-option label="多选题" value="多选题"></el-option>
    <el-option label="判断题" value="判断题"></el-option>
    <el-option label="填空题" value="填空题"></el-option>
    <el-option label="简答题" value="简答题"></el-option>
  </el-select>
</el-form-item>
```

## 配置检查

### DeepSeek API配置
确保在`application.properties`中正确配置了API密钥和URL：

```properties
# API配置
deepseek.api.url=https://api.deepseek.com/chat/completions
deepseek.api.key=${DEEPSEEK_API_KEY:}
```

### 网络连接测试
系统会在启动时自动测试与DeepSeek API的网络连接，如果连接失败会在日志中记录详细信息。

## 测试建议

### 1. 测试AI一键出卷功能
1. 启动后端服务
2. 在前端访问"AI一键出卷"功能
3. 填写考试信息并点击"开始生成试卷"
4. 检查后端日志，确认是否真正调用了DeepSeek API
5. 如果出现错误，检查API密钥和网络连接

### 2. 测试AI一键出题功能
1. 在前端访问"AI智能出题"功能
2. 选择学科、题型等参数
3. 点击"开始智能出题"
4. 检查生成的题目是否包含正确的题型信息
5. 编辑题目并保存，确认题型信息正确保存

## 故障排除

### 如果API调用仍然失败：
1. 检查API密钥是否正确
2. 检查网络连接是否正常
3. 检查DeepSeek API服务是否可用
4. 查看后端日志获取详细错误信息

### 如果题目类型仍然丢失：
1. 检查前端表单中是否选择了题型
2. 检查浏览器控制台是否有JavaScript错误
3. 检查后端日志中的题目保存过程
4. 确认数据库中题目类型字段的存储情况

## 注意事项

1. **API密钥安全**：请确保API密钥的安全性，不要在公开代码中暴露
2. **错误处理**：修复后系统会更严格地处理API错误，可能需要处理更多的异常情况
3. **用户体验**：由于移除了模拟数据，API调用失败时用户会看到真实的错误信息
4. **性能考虑**：真实的API调用可能比模拟数据慢，需要适当的加载提示

## 后续改进建议

1. 添加API调用重试机制
2. 实现API调用缓存，避免重复请求
3. 添加更详细的错误分类和处理
4. 实现API调用监控和统计
5. 考虑添加备用AI服务提供商
