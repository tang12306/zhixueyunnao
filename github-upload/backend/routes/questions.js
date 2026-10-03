const express = require('express');
const router = express.Router();
const multer = require('multer');
const XLSX = require('xlsx');
const Question = require('../models/Question');
const auth = require('../middlewares/auth');

// 配置multer用于文件上传
const storage = multer.memoryStorage();
const upload = multer({ 
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 限制5MB
  fileFilter: (req, file, cb) => {
    if (file.mimetype.includes('excel') || 
        file.mimetype.includes('spreadsheetml') ||
        file.originalname.endsWith('.xlsx') ||
        file.originalname.endsWith('.xls')) {
      cb(null, true);
    } else {
      cb(new Error('文件类型必须是Excel文件'), false);
    }
  }
});

// @route   GET /api/questions
// @desc    获取所有题目
// @access  Private
router.get('/', auth, async (req, res) => {
  try {
    const { subject, type, difficulty, search, page = 1, limit = 20 } = req.query;
    const query = {};
    
    // 应用过滤条件
    if (subject) query.subject = subject;
    if (type) query.type = type;
    if (difficulty) query.difficulty = parseInt(difficulty);
    if (search) {
      query.$or = [
        { content: { $regex: search, $options: 'i' } },
        { tags: { $regex: search, $options: 'i' } }
      ];
    }
    
    // 分页查询
    const skip = (parseInt(page) - 1) * parseInt(limit);
    
    const questions = await Question.find(query)
      .skip(skip)
      .limit(parseInt(limit))
      .sort({ createdAt: -1 })
      .populate('createdBy', 'name');
      
    const total = await Question.countDocuments(query);
    
    res.json({
      success: true,
      questions,
      pagination: {
        total,
        page: parseInt(page),
        limit: parseInt(limit),
        pages: Math.ceil(total / parseInt(limit))
      }
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

// @route   GET /api/questions/:id
// @desc    根据ID获取题目
// @access  Private
router.get('/:id', auth, async (req, res) => {
  try {
    const question = await Question.findById(req.params.id)
      .populate('createdBy', 'name');
      
    if (!question) {
      return res.status(404).json({
        success: false,
        message: '未找到该题目'
      });
    }
    
    res.json({
      success: true,
      question
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

// @route   POST /api/questions
// @desc    创建新题目
// @access  Private
router.post('/', auth, async (req, res) => {
  try {
    const { subject, type, difficulty, content, options, answer, analysis, tags } = req.body;
    
    const newQuestion = new Question({
      subject,
      type,
      difficulty,
      content,
      options: options || [],
      answer,
      analysis,
      tags: tags || [],
      createdBy: req.user.id
    });
    
    await newQuestion.save();
    
    res.status(201).json({
      success: true,
      message: '题目创建成功',
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

// @route   PUT /api/questions/:id
// @desc    更新题目
// @access  Private
router.put('/:id', auth, async (req, res) => {
  try {
    const { subject, type, difficulty, content, options, answer, analysis, tags } = req.body;
    
    // 构建更新对象
    const questionFields = {};
    if (subject) questionFields.subject = subject;
    if (type) questionFields.type = type;
    if (difficulty) questionFields.difficulty = difficulty;
    if (content) questionFields.content = content;
    if (options) questionFields.options = options;
    if (answer) questionFields.answer = answer;
    if (analysis) questionFields.analysis = analysis;
    if (tags) questionFields.tags = tags;
    
    // 查找并更新题目
    let question = await Question.findById(req.params.id);
    
    if (!question) {
      return res.status(404).json({
        success: false,
        message: '未找到该题目'
      });
    }
    
    // 检查权限（只有创建者或管理员可以更新）
    if (question.createdBy.toString() !== req.user.id && req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: '没有权限修改此题目'
      });
    }
    
    question = await Question.findByIdAndUpdate(
      req.params.id,
      { $set: questionFields },
      { new: true }
    );
    
    res.json({
      success: true,
      message: '题目更新成功',
      question
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

// @route   DELETE /api/questions/:id
// @desc    删除题目
// @access  Private
router.delete('/:id', auth, async (req, res) => {
  try {
    // 查找题目
    const question = await Question.findById(req.params.id);
    
    if (!question) {
      return res.status(404).json({
        success: false,
        message: '未找到该题目'
      });
    }
    
    // 检查权限（只有创建者或管理员可以删除）
    if (question.createdBy.toString() !== req.user.id && req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: '没有权限删除此题目'
      });
    }
    
    await Question.findByIdAndRemove(req.params.id);
    
    res.json({
      success: true,
      message: '题目删除成功'
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

// @route   POST /api/questions/import
// @desc    从Excel导入题目
// @access  Private
router.post('/import', [auth, upload.single('file')], async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({
        success: false,
        message: '请上传Excel文件'
      });
    }
    
    // 解析Excel文件
    const workbook = XLSX.read(req.file.buffer, { type: 'buffer' });
    const sheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[sheetName];
    const data = XLSX.utils.sheet_to_json(worksheet);
    
    if (data.length === 0) {
      return res.status(400).json({
        success: false,
        message: 'Excel文件为空或格式不正确'
      });
    }
    
    // 验证必要字段
    const requiredFields = ['subject', 'type', 'difficulty', 'content'];
    const missingFields = [];
    
    for (const field of requiredFields) {
      if (!data[0].hasOwnProperty(field)) {
        missingFields.push(field);
      }
    }
    
    if (missingFields.length > 0) {
      return res.status(400).json({
        success: false,
        message: `Excel文件缺少必要字段: ${missingFields.join(', ')}`
      });
    }
    
    // 处理导入数据
    const questions = [];
    const errors = [];
    
    for (let i = 0; i < data.length; i++) {
      const item = data[i];
      
      try {
        // 验证题目类型
        const validTypes = ['单选题', '多选题', '判断题', '填空题', '简答题', '作文题', '阅读理解'];
        if (!validTypes.includes(item.type)) {
          errors.push(`第${i+1}行: 题目类型无效`);
          continue;
        }
        
        // 验证科目
        const validSubjects = ['语文', '数学', '英语'];
        if (!validSubjects.includes(item.subject)) {
          errors.push(`第${i+1}行: 科目无效`);
          continue;
        }
        
        // 构建题目对象
        const questionData = {
          subject: item.subject,
          type: item.type,
          difficulty: parseInt(item.difficulty) || 1,
          content: item.content,
          answer: item.answer || '',
          analysis: item.analysis || '',
          tags: item.tags ? item.tags.split(',').map(tag => tag.trim()) : [],
          createdBy: req.user.id
        };
        
        // 处理选项（如果有）
        if (item.options) {
          const optionsStr = item.options;
          const optionsArr = optionsStr.split('|').map(opt => opt.trim());
          const correctOptions = item.correctOptions ? item.correctOptions.split(',').map(opt => opt.trim()) : [];
          
          questionData.options = optionsArr.map(optText => {
            return {
              text: optText,
              isCorrect: correctOptions.includes(optText)
            };
          });
        }
        
        questions.push(questionData);
      } catch (error) {
        errors.push(`第${i+1}行: ${error.message}`);
      }
    }
    
    if (questions.length === 0) {
      return res.status(400).json({
        success: false,
        message: '没有有效的题目数据可导入',
        errors
      });
    }
    
    // 批量导入题目
    const savedQuestions = await Question.insertMany(questions);
    
    res.status(201).json({
      success: true,
      message: `成功导入 ${savedQuestions.length} 道题目${errors.length > 0 ? `, ${errors.length} 条记录有错误` : ''}`,
      importedCount: savedQuestions.length,
      totalRecords: data.length,
      errors: errors.length > 0 ? errors : null
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

module.exports = router; 