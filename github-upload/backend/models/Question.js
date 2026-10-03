const mongoose = require('mongoose');

const OptionSchema = new mongoose.Schema({
  text: {
    type: String,
    required: true
  },
  isCorrect: {
    type: Boolean,
    default: false
  }
});

const QuestionSchema = new mongoose.Schema({
  subject: {
    type: String,
    required: true,
    enum: ['语文', '数学', '英语']
  },
  type: {
    type: String,
    required: true,
    enum: ['单选题', '多选题', '判断题', '填空题', '简答题', '作文题', '阅读理解']
  },
  difficulty: {
    type: Number,
    required: true,
    min: 1,
    max: 5
  },
  content: {
    type: String,
    required: true
  },
  options: [OptionSchema],
  answer: {
    type: String
  },
  analysis: {
    type: String
  },
  tags: [String],
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

// 更新时间中间件
QuestionSchema.pre('findOneAndUpdate', function(next) {
  this.set({ updatedAt: new Date() });
  next();
});

module.exports = mongoose.model('Question', QuestionSchema); 