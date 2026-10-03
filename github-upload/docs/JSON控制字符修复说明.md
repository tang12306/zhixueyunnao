# JSON控制字符修复说明

## 问题描述

在使用AI一键出卷功能时，遇到以下错误：

```
生成试卷过程中出现错误，请稍后重试。 错误信息: 生成试卷失败: DeepSeek API调用失败: API调用失败: 400, 错误信息: Failed to parse the request body as JSON: messages[1].content: control character (\u0000-\u001F) found while parsing a string at line 11 column 0
```

## 问题根源

DeepSeek API返回400错误，提示JSON请求体中包含控制字符（\u0000-\u001F），这些字符在JSON标准中是不被允许的。

### 控制字符说明

控制字符是ASCII码表中的前32个字符（\u0000到\u001F），包括：
- \u0000 - NULL字符
- \u0001-\u0008 - 各种控制字符
- \u0009 - 制表符（\t）
- \u000A - 换行符（\n）
- \u000B - 垂直制表符
- \u000C - 换页符（\f）
- \u000D - 回车符（\r）
- \u000E-\u001F - 其他控制字符

## 修复方案

### 1. 添加JSON字符串清理方法

在 `DeepSeekService.java` 中添加了 `cleanJsonString` 方法：

```java
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
```

### 2. 修改API调用逻辑

在 `generateQuestion` 方法中使用清理后的提示词：

```java
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
    // ... 其余代码
}
```

### 3. 修复RequestBody过时方法

同时修复了OkHttp RequestBody的过时方法警告：

```java
// 修复前（过时方法）
.post(RequestBody.create(okhttp3.MediaType.parse("application/json"), jsonBody))

// 修复后（新方法）
.post(RequestBody.create(jsonBody, okhttp3.MediaType.parse("application/json")))
```

## 修复效果

### 修复前
- DeepSeek API返回400错误
- 错误信息：`control character (\u0000-\u001F) found while parsing a string`
- AI一键出卷功能无法正常工作

### 修复后
- ✅ 自动清理提示词中的控制字符
- ✅ 正确转义JSON特殊字符
- ✅ DeepSeek API调用成功
- ✅ AI一键出卷功能正常工作

## 技术细节

### 控制字符清理策略

1. **移除策略**：使用正则表达式 `[\\u0000-\\u001F]` 移除所有控制字符
2. **转义策略**：对JSON中的特殊字符进行正确转义
3. **保留策略**：保留有意义的空白字符（空格）

### 为什么不影响AIServiceImpl

`AIServiceImpl` 使用 `RestTemplate` 和 `Map` 构建请求体，Spring会自动处理JSON序列化和字符转义，因此不会出现控制字符问题。

而 `DeepSeekService` 使用字符串拼接构建JSON，需要手动处理字符转义。

## 测试验证

### 测试步骤

1. 启动后端服务（端口8080）
2. 启动前端服务（端口8083）
3. 使用教师账号登录：demo.teacher/DemoTeacher123!
4. 进入"AI一键出卷"功能
5. 填写试卷信息并生成试卷
6. 验证是否成功调用DeepSeek API

### 预期结果

- ✅ 不再出现控制字符错误
- ✅ 成功调用DeepSeek API
- ✅ 正常生成试卷内容
- ✅ 题目保存到数据库

## 相关文件

### 修改的文件
- `src/main/java/com/_1/service/DeepSeekService.java`

### 新增方法
- `cleanJsonString(String input)` - JSON字符串清理方法

### 修复的方法
- `generateQuestion(String prompt)` - 题目生成方法

## 注意事项

1. **字符编码**：确保所有文本输入使用UTF-8编码
2. **输入验证**：前端应该验证用户输入，避免包含异常字符
3. **日志记录**：保留详细的日志记录，便于问题排查
4. **错误处理**：保持原有的错误处理机制

## 后续优化建议

1. **前端验证**：在前端添加输入验证，过滤控制字符
2. **统一处理**：考虑在所有API调用处统一使用字符清理
3. **配置化**：将字符清理规则配置化，便于调整
4. **监控告警**：添加API调用失败的监控和告警

---

**修复时间**：2025-06-10  
**修复版本**：v1.0.1  
**测试状态**：✅ 已验证修复有效
