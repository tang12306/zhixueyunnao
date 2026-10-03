const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const dotenv = require('dotenv');
const path = require('path');

// 配置环境变量
dotenv.config();

// 创建Express应用
const app = express();

// 中间件配置
app.use(cors({
  origin: ['http://localhost:8083', 'http://localhost:8084'],
  credentials: true,
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'x-auth-token']
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 数据库连接
mongoose.connect(process.env.MONGODB_URI || 'mongodb://localhost:27017/education-question-bank')
  .then(() => {
    console.log('数据库连接成功');
    
    // 尝试初始化默认设置 (只在成功连接数据库后执行)
    const { initializeDefaultSettings } = require('./routes/settings');
    initializeDefaultSettings().catch(err => console.error('Error initializing default settings during app start:', err));
  })
  .catch((err) => {
    console.error('数据库连接失败:', err);
    console.warn('程序将继续运行，但依赖数据库的功能将不可用');
    // 不再调用 process.exit(1)，让程序继续运行
  });

// API路由
const authRoutes = require('./routes/auth');
const questionRoutes = require('./routes/questions');
const paperRoutes = require('./routes/papers');
const aiRoutes = require('./routes/ai');
const settingsRoutes = require('./routes/settings');

// 从app.js主程序中移除对initializeDefaultSettings的调用
// 它现在只在数据库连接成功后才会被调用

app.use('/api/auth', authRoutes);
app.use('/api/questions', questionRoutes);
app.use('/api/papers', paperRoutes);
app.use('/api/ai', aiRoutes);
app.use('/api/settings', settingsRoutes);

// 错误处理中间件
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    success: false,
    message: '服务器内部错误',
    error: process.env.NODE_ENV === 'development' ? err.message : null
  });
});

// 启动服务器
const PORT = process.env.PORT || 5000;
app.listen(PORT, () => {
  console.log(`服务器运行在端口 ${PORT}`);
}); 