const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const User = require('../models/User');

// 中间件 - 验证用户身份
const auth = require('../middlewares/auth');

// @route   POST /api/auth/register
// @desc    注册新用户
// @access  Public
router.post('/register', async (req, res) => {
  try {
    const { username, password, name, role } = req.body;

    // 检查用户是否已存在
    let user = await User.findOne({ username });
    if (user) {
      return res.status(400).json({ 
        success: false, 
        message: '用户名已存在' 
      });
    }

    // 创建新用户
    user = new User({
      username,
      password,
      name,
      role: role || 'teacher' // 默认为教师角色
    });

    await user.save();

    // 生成 JWT token
    const token = jwt.sign(
      { id: user.id, role: user.role },
      process.env.JWT_SECRET || 'your_jwt_secret',
      { expiresIn: '7d' }
    );

    res.status(201).json({
      success: true,
      token,
      user: {
        id: user.id,
        username: user.username,
        name: user.name,
        role: user.role
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

// @route   POST /api/auth/login
// @desc    用户登录
// @access  Public
router.post('/login', async (req, res) => {
  try {
    const { username, password } = req.body;

    // 检查用户是否存在
    const user = await User.findOne({ username });
    if (!user) {
      return res.status(400).json({ 
        success: false, 
        message: '用户名或密码不正确' 
      });
    }

    // 验证密码
    const isMatch = await user.comparePassword(password);
    if (!isMatch) {
      return res.status(400).json({ 
        success: false, 
        message: '用户名或密码不正确' 
      });
    }

    // 更新最后登录时间
    user.lastLogin = Date.now();
    await user.save();

    // 生成 JWT token
    const token = jwt.sign(
      { id: user.id, role: user.role },
      process.env.JWT_SECRET || 'your_jwt_secret',
      { expiresIn: '7d' }
    );

    res.json({
      success: true,
      token,
      user: {
        id: user.id,
        username: user.username,
        name: user.name,
        role: user.role
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

// @route   GET /api/auth/me
// @desc    获取当前用户信息
// @access  Private
router.get('/me', auth, async (req, res) => {
  try {
    const user = await User.findById(req.user.id).select('-password');
    if (!user) {
      return res.status(404).json({ 
        success: false, 
        message: '用户不存在' 
      });
    }
    
    res.json({
      success: true,
      user
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

// @route   PUT /api/auth/profile
// @desc    更新当前用户信息 (例如 name, email)
// @access  Private
router.put('/profile', auth, async (req, res) => {
  try {
    const { name, email } = req.body;
    const userId = req.user.id;

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ success: false, message: '用户不存在' });
    }

    // 更新允许修改的字段
    if (name) user.name = name;
    if (email) user.email = email; // email 字段已在 User 模型中添加

    // 使用 { new: true, runValidators: true } 确保返回更新后的文档并运行模型校验
    const updatedUser = await user.save({ new: true, runValidators: true });
    // 或者使用 findByIdAndUpdate
    // const updatedUser = await User.findByIdAndUpdate(
    //   userId,
    //   { $set: { name, email } },
    //   { new: true, runValidators: true, context: 'query' }
    // ).select('-password');

    // 从返回结果中移除密码
    const userResponse = updatedUser.toObject();
    delete userResponse.password;

    res.json({
      success: true,
      message: '用户信息更新成功',
      user: userResponse
    });

  } catch (err) {
    console.error('Error updating profile:', err);
    if (err.name === 'ValidationError') {
      return res.status(400).json({ success: false, message: '更新失败，校验错误', errors: err.errors });
    }
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// @route   POST /api/auth/change-password
// @desc    修改当前用户密码
// @access  Private
router.post('/change-password', auth, async (req, res) => {
  try {
    const { oldPassword, newPassword } = req.body;
    const userId = req.user.id;

    if (!oldPassword || !newPassword) {
      return res.status(400).json({ success: false, message: '旧密码和新密码不能为空' });
    }

    if (newPassword.length < 6) { // 假设密码最小长度为6
        return res.status(400).json({ success: false, message: '新密码长度不能少于6位' });
    }

    const user = await User.findById(userId);
    if (!user) {
      return res.status(404).json({ success: false, message: '用户不存在' });
    }

    // 验证旧密码
    const isMatch = await user.comparePassword(oldPassword);
    if (!isMatch) {
      return res.status(400).json({ success: false, message: '旧密码不正确' });
    }

    // 更新密码 (User模型的 pre('save') 中间件会自动加密)
    user.password = newPassword;
    await user.save();

    res.json({ success: true, message: '密码修改成功' });

  } catch (err) {
    console.error('Error changing password:', err);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

module.exports = router; 