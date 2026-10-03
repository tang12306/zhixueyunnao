const mongoose = require('mongoose');

const PaperQuestionSchema = new mongoose.Schema({
  question: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Question',
    required: true
  },
  score: {
    type: Number,
    required: true,
    min: 1
  }
});

const PaperSchema = new mongoose.Schema({
  title: {
    type: String,
    required: true
  },
  subject: {
    type: String,
    required: true,
    enum: ['语文', '数学', '英语']
  },
  description: {
    type: String
  },
  questions: [PaperQuestionSchema],
  totalScore: {
    type: Number,
    default: 0
  },
  duration: {
    type: Number, // 分钟
    default: 60
  },
  isTemplate: {
    type: Boolean,
    default: false
  },
  createdBy: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  updatedAt: {
    type: Date,
    default: Date.now
  }
});

// 自动计算总分
PaperSchema.pre('save', function(next) {
  this.totalScore = this.questions.reduce((sum, q) => sum + q.score, 0);
  next();
});

// 更新时间中间件
PaperSchema.pre('findOneAndUpdate', function(next) {
  this.set({ updatedAt: new Date() });
  next();
});

module.exports = mongoose.model('Paper', PaperSchema); 