const express = require('express');
const router = express.Router();
const axios = require('axios');
const auth = require('../middlewares/auth');
const Question = require('../models/Question');

// AI模型参数
const DEEPSEEK_API_URL = process.env.DEEPSEEK_API_URL || 'https://api.deepseek.com/v1/chat/completions';
const DEEPSEEK_API_KEY = process.env.DEEPSEEK_API_KEY;

// 辅助函数：调用DeepSeek API
const callDeepSeekAPI = async (prompt) => {
  try {
    if (!DEEPSEEK_API_KEY) {
      throw new Error('DeepSeek API密钥未配置');
    }

    const response = await axios.post(
      DEEPSEEK_API_URL,
      {
        model: 'deepseek-chat',
        messages: [
          { role: 'system', content: '你是一个专业的教育题目生成助手，擅长创建高质量的教育题目。' },
          { role: 'user', content: prompt }
        ],
        temperature: 0.7,
        max_tokens: 2000
      },
      {
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${DEEPSEEK_API_KEY}`
        }
      }
    );

    return response.data.choices[0].message.content;
  } catch (error) {
    console.error('调用DeepSeek API失败:', error);
    throw new Error('AI服务暂时不可用，请稍后再试');
  }
};

// @route   POST /api/ai/generate-question
// @desc    使用AI生成题目
// @access  Private
router.post('/generate-question', auth, async (req, res) => {
  try {
    const { subject, type, difficulty, tags, topic, count = 1 } = req.body;

    if (!subject || !type) {
      return res.status(400).json({
        success: false,
        message: '科目和题型是必须的'
      });
    }

    // 根据请求构建提示词
    let prompt = `请为我生成${count}道${subject}学科的${type}，`;
    
    if (difficulty) {
      prompt += `难度级别为${difficulty}/5，`;
    }
    
    if (topic) {
      prompt += `主题或知识点为"${topic}"，`;
    }
    
    if (tags && tags.length > 0) {
      prompt += `涉及的标签有: ${tags.join(', ')}，`;
    }

    // 添加具体的要求和格式
    prompt += `
请按照以下JSON格式返回题目，不要有任何其他文字或解释：
[
  {
    "content": "题目内容",
    "options": ["选项A", "选项B", "选项C", "选项D"], // 仅选择题需要
    "answer": "正确答案",
    "analysis": "解析和解答思路"
  }
]`;

    // 调用AI生成题目
    const aiResponse = await callDeepSeekAPI(prompt);
    
    // 尝试解析JSON响应
    let questions = [];
    try {
      // 提取JSON部分
      const jsonMatch = aiResponse.match(/\[[\s\S]*\]/);
      if (jsonMatch) {
        questions = JSON.parse(jsonMatch[0]);
      } else {
        throw new Error('无法从AI响应中提取JSON');
      }
    } catch (error) {
      console.error('解析AI响应失败:', error, aiResponse);
      return res.status(500).json({
        success: false,
        message: 'AI生成的题目格式不正确',
        aiResponse
      });
    }

    res.json({
      success: true,
      questions,
      rawResponse: aiResponse
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({
      success: false,
      message: err.message || '服务器错误',
      error: process.env.NODE_ENV === 'development' ? err.message : null
    });
  }
});

// @route   POST /api/ai/save-question
// @desc    保存AI生成的题目
// @access  Private
router.post('/save-question', auth, async (req, res) => {
  try {
    const { subject, type, difficulty, content, options, answer, analysis, tags } = req.body;
    
    if (!subject || !type || !content) {
      return res.status(400).json({
        success: false,
        message: '科目、题型和内容是必须的'
      });
    }
    
    // 处理选项数据
    let processedOptions = [];
    if (options && Array.isArray(options)) {
      if (type === '单选题' || type === '多选题') {
        let correctOptions = [];
        if (type === '单选题') {
          correctOptions = [answer];
        } else if (type === '多选题' && answer) {
          correctOptions = answer.split(',').map(opt => opt.trim());
        }
        
        processedOptions = options.map(opt => ({
          text: opt,
          isCorrect: correctOptions.includes(opt)
        }));
      }
    }
    
    // 创建新题目
    const newQuestion = new Question({
      subject,
      type,
      difficulty: parseInt(difficulty) || 3,
      content,
      options: processedOptions,
      answer,
      analysis,
      tags: tags || [],
      createdBy: req.user.id
    });
    
    await newQuestion.save();
    
    res.status(201).json({
      success: true,
      message: 'AI生成的题目已保存',
      question: newQuestion
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({
      success: false,
      message: '服务器错误',
      error: process.env.NODE_ENV === 'development' ? err.message : null
    });
  }
});

// @route   POST /api/ai/improve-question
// @desc    使用AI优化题目
// @access  Private
router.post('/improve-question', auth, async (req, res) => {
  try {
    const { questionId, instruction } = req.body;
    
    if (!questionId) {
      return res.status(400).json({
        success: false,
        message: '题目ID是必须的'
      });
    }
    
    // 获取题目信息
    const question = await Question.findById(questionId);
    if (!question) {
      return res.status(404).json({
        success: false,
        message: '未找到该题目'
      });
    }
    
    // 构建提示词
    let prompt = `请优化改进以下${question.subject}学科的${question.type}：\n\n`;
    prompt += `题目内容：${question.content}\n\n`;
    
    if (question.options && question.options.length > 0) {
      prompt += '选项：\n';
      question.options.forEach((opt, index) => {
        prompt += `${String.fromCharCode(65 + index)}. ${opt.text}\n`;
      });
      prompt += '\n';
    }
    
    if (question.answer) {
      prompt += `答案：${question.answer}\n\n`;
    }
    
    if (question.analysis) {
      prompt += `解析：${question.analysis}\n\n`;
    }
    
    prompt += `优化要求：${instruction || '提高题目质量，修正错误，使表述更清晰准确'}\n\n`;
    
    prompt += `请按照以下JSON格式返回优化后的题目，不要有任何其他文字或解释：
{
  "content": "优化后的题目内容",
  "options": ["优化后的选项A", "优化后的选项B", "优化后的选项C", "优化后的选项D"], // 仅选择题需要
  "answer": "优化后的正确答案",
  "analysis": "优化后的解析和解答思路"
}`;
    
    // 调用AI优化题目
    const aiResponse = await callDeepSeekAPI(prompt);
    
    // 尝试解析JSON响应
    let improvedQuestion = {};
    try {
      // 提取JSON部分
      const jsonMatch = aiResponse.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        improvedQuestion = JSON.parse(jsonMatch[0]);
      } else {
        throw new Error('无法从AI响应中提取JSON');
      }
    } catch (error) {
      console.error('解析AI响应失败:', error, aiResponse);
      return res.status(500).json({
        success: false,
        message: 'AI生成的优化内容格式不正确',
        aiResponse
      });
    }
    
    res.json({
      success: true,
      originalQuestion: question,
      improvedQuestion,
      rawResponse: aiResponse
    });
  } catch (err) {
    console.error(err);
    res.status(500).json({
      success: false,
      message: err.message || '服务器错误',
      error: process.env.NODE_ENV === 'development' ? err.message : null
    });
  }
});

module.exports = router; 