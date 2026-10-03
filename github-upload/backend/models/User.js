const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const UserSchema = new mongoose.Schema({
  username: {
    type: String,
    required: true,
    unique: true
  },
  password: {
    type: String,
    required: true
  },
  name: {
    type: String,
    required: true
  },
  email: {
    type: String,
    trim: true,
    // unique: true, // 可选：如果需要邮箱唯一
    // sparse: true, // 配合 unique: true，允许多个文档的 email 为 null/undefined
    match: [/.+@.+\..+/, '请输入有效的邮箱地址'] // 简单的邮箱格式校验
  },
  role: {
    type: String,
    enum: ['admin', 'teacher'],
    default: 'teacher'
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  lastLogin: {
    type: Date
  }
});

// 密码加密中间件
UserSchema.pre('save', async function(next) {
  if (!this.isModified('password')) {
    return next();
  }
  
  try {
    const salt = await bcrypt.genSalt(10);
    this.password = await bcrypt.hash(this.password, salt);
    next();
  } catch (error) {
    next(error);
  }
});

// 验证密码方法
UserSchema.methods.comparePassword = async function(password) {
  return bcrypt.compare(password, this.password);
};

module.exports = mongoose.model('User', UserSchema); 