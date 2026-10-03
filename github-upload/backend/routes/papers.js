const express = require('express');
const router = express.Router();
const Question = require('../models/Question');
const Paper = require('../models/Paper');
const auth = require('../middlewares/auth');

// @route   GET /api/papers
// @desc    获取所有试卷
// @access  Private
router.get('/', auth, async (req, res) => {
  try {
    const { subject, search, page = 1, limit = 20 } = req.query;
    const query = {};
    
    // 应用过滤条件
    if (subject) query.subject = subject;
    if (search) {
      query.$or = [
        { title: { $regex: search, $options: 'i' } },
        { description: { $regex: search, $options: 'i' } }
      ];
    }
    
    // 分页查询
    const skip = (parseInt(page) - 1) * parseInt(limit);
    
    const papers = await Paper.find(query)
      .skip(skip)
      .limit(parseInt(limit))
      .sort({ createdAt: -1 })
      .populate('createdBy', 'name');
      
    const total = await Paper.countDocuments(query);
    
    res.json({
      success: true,
      papers,
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

// @route   GET /api/papers/:id
// @desc    根据ID获取试卷
// @access  Private
router.get('/:id', auth, async (req, res) => {
  try {
    const paper = await Paper.findById(req.params.id)
      .populate('createdBy', 'name')
      .populate('questions.question');
      
    if (!paper) {
      return res.status(404).json({
        success: false,
        message: '未找到该试卷'
      });
    }
    
    res.json({
      success: true,
      paper
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

// @route   POST /api/papers
// @desc    创建新试卷
// @access  Private
router.post('/', auth, async (req, res) => {
  try {
    const { title, subject, description, questions, duration, isTemplate } = req.body;
    
    // 验证题目ID是否存在
    if (questions && questions.length > 0) {
      const questionIds = questions.map(q => q.question);
      const foundQuestions = await Question.find({ _id: { $in: questionIds } });
      
      if (foundQuestions.length !== questionIds.length) {
        return res.status(400).json({
          success: false,
          message: '某些题目ID无效'
        });
      }
    }
    
    const newPaper = new Paper({
      title,
      subject,
      description,
      questions: questions || [],
      duration,
      isTemplate: isTemplate || false,
      createdBy: req.user.id
    });
    
    await newPaper.save();
    
    res.status(201).json({
      success: true,
      message: '试卷创建成功',
      paper: newPaper
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

// @route   PUT /api/papers/:id
// @desc    更新试卷
// @access  Private
router.put('/:id', auth, async (req, res) => {
  try {
    const { title, subject, description, questions, duration, isTemplate } = req.body;
    
    // 构建更新对象
    const paperFields = {};
    if (title) paperFields.title = title;
    if (subject) paperFields.subject = subject;
    if (description) paperFields.description = description;
    if (duration) paperFields.duration = duration;
    if (isTemplate !== undefined) paperFields.isTemplate = isTemplate;
    
    // 验证题目ID是否存在
    if (questions && questions.length > 0) {
      const questionIds = questions.map(q => q.question);
      const foundQuestions = await Question.find({ _id: { $in: questionIds } });
      
      if (foundQuestions.length !== questionIds.length) {
        return res.status(400).json({
          success: false,
          message: '某些题目ID无效'
        });
      }
      
      paperFields.questions = questions;
    }
    
    // 查找并更新试卷
    let paper = await Paper.findById(req.params.id);
    
    if (!paper) {
      return res.status(404).json({
        success: false,
        message: '未找到该试卷'
      });
    }
    
    // 检查权限（只有创建者或管理员可以更新）
    if (paper.createdBy.toString() !== req.user.id && req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: '没有权限修改此试卷'
      });
    }
    
    paper = await Paper.findByIdAndUpdate(
      req.params.id,
      { $set: paperFields },
      { new: true }
    );
    
    res.json({
      success: true,
      message: '试卷更新成功',
      paper
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

// @route   DELETE /api/papers/:id
// @desc    删除试卷
// @access  Private
router.delete('/:id', auth, async (req, res) => {
  try {
    // 查找试卷
    const paper = await Paper.findById(req.params.id);
    
    if (!paper) {
      return res.status(404).json({
        success: false,
        message: '未找到该试卷'
      });
    }
    
    // 检查权限（只有创建者或管理员可以删除）
    if (paper.createdBy.toString() !== req.user.id && req.user.role !== 'admin') {
      return res.status(403).json({
        success: false,
        message: '没有权限删除此试卷'
      });
    }
    
    await Paper.findByIdAndRemove(req.params.id);
    
    res.json({
      success: true,
      message: '试卷删除成功'
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

// @route   POST /api/papers/generate
// @desc    自动生成试卷
// @access  Private
router.post('/generate', auth, async (req, res) => {
  try {
    const { 
      title, 
      subject, 
      description,
      questionCriteria,
      totalScore = 100,
      duration = 60
    } = req.body;
    
    if (!subject || !questionCriteria || questionCriteria.length === 0) {
      return res.status(400).json({
        success: false,
        message: '缺少必要参数：科目和题目标准'
      });
    }
    
    // 检查传入的题目要求，格式为：
    // [
    //   { type: '单选题', count: 10, score: 3, difficulty: { min: 1, max: 3 } },
    //   { type: '填空题', count: 5, score: 5, difficulty: { min: 2, max: 4 } },
    //   ...
    // ]
    
    let generatedQuestions = [];
    let actualTotalScore = 0;
    
    // 根据每种题型要求，随机选择符合条件的题目
    for (const criteria of questionCriteria) {
      const { type, count, score, difficulty, tags } = criteria;
      
      // 构建查询条件
      const query = { subject, type };
      
      // 添加难度范围条件
      if (difficulty && (difficulty.min || difficulty.max)) {
        query.difficulty = {};
        if (difficulty.min) query.difficulty.$gte = difficulty.min;
        if (difficulty.max) query.difficulty.$lte = difficulty.max;
      }
      
      // 添加标签条件
      if (tags && tags.length > 0) {
        query.tags = { $in: tags };
      }
      
      // 随机选择指定数量的题目
      const questions = await Question.aggregate([
        { $match: query },
        { $sample: { size: count } }
      ]);
      
      // 添加到生成的题目列表，并计算实际总分
      questions.forEach(question => {
        generatedQuestions.push({
          question: question._id,
          score
        });
        actualTotalScore += score;
      });
      
      // 如果没有足够的题目满足条件
      if (questions.length < count) {
        return res.status(400).json({
          success: false,
          message: `无法找到足够的${type}题目，请调整条件或增加题库`
        });
      }
    }
    
    // 创建新试卷
    const newPaper = new Paper({
      title: title || `${subject}自动生成试卷-${new Date().toLocaleDateString()}`,
      subject,
      description: description || `自动生成的${subject}试卷，总分${actualTotalScore}分，时长${duration}分钟`,
      questions: generatedQuestions,
      duration,
      createdBy: req.user.id
    });
    
    await newPaper.save();
    
    res.status(201).json({
      success: true,
      message: '试卷生成成功',
      paper: newPaper
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