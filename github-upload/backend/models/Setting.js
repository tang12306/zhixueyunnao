const mongoose = require('mongoose');

const SettingSchema = new mongoose.Schema({
  key: {
    type: String,
    required: [true, '设置键 (key) 不能为空'],
    unique: true,
    trim: true
  },
  value: {
    type: mongoose.Schema.Types.Mixed, // 可以存储任何类型的值
    required: [true, '设置值 (value) 不能为空']
  },
  name: {
    type: String, // 用户友好的设置名称
    trim: true
  },
  description: {
    type: String, // 设置的描述
    trim: true
  },
  category: {
    type: String, // 设置分类，例如 'API Keys', 'General', 'AI Configuration'
    trim: true,
    default: 'General'
  },
  isEditable: {
    type: Boolean, // 是否允许用户通过UI编辑
    default: true
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

// 更新 updatedAt 时间戳的中间件
SettingSchema.pre('save', function(next) {
  this.updatedAt = Date.now();
  next();
});

// 如果使用 findByIdAndUpdate, findOneAndUpdate 等，也需要中间件来更新updatedAt
// SettingSchema.pre('findOneAndUpdate', function(next) {
//   this.set({ updatedAt: Date.now() });
//   next();
// });

module.exports = mongoose.model('Setting', SettingSchema); 