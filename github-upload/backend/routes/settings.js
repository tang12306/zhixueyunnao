const express = require('express');
const router = express.Router();
const Setting = require('../models/Setting');
const auth = require('../middlewares/auth'); // 身份验证中间件
// const adminAuth = require('../middlewares/adminAuth'); // 管理员权限中间件 (如果个人中心设置只允许admin)

// 辅助函数：初始化默认设置 (如果不存在)
const initializeDefaultSettings = async () => {
  const defaultSettings = [
    {
      key: 'DEEPSEEK_API_KEY',
      value: 'your_deepseek_api_key_here',
      name: 'DeepSeek API 密钥',
      description: '用于调用 DeepSeek AI 服务的 API 密钥。',
      category: 'API Keys',
      isEditable: true
    },
    {
      key: 'DEEPSEEK_API_URL',
      value: 'https://api.deepseek.com/chat/completions',
      name: 'DeepSeek API 地址',
      description: 'DeepSeek AI 服务的 API 端点 URL。',
      category: 'API Keys',
      isEditable: true
    },
    {
        key: 'SITE_NAME',
        value: '教务题库管理系统',
        name: '系统名称',
        description: '显示在浏览器标签页和各处标题的系统名称',
        category: 'General',
        isEditable: true
    }
    // 可以添加更多默认设置项
  ];

  for (const setting of defaultSettings) {
    const existing = await Setting.findOne({ key: setting.key });
    if (!existing) {
      await Setting.create(setting);
      console.log(`Default setting created: ${setting.key}`);
    }
  }
};

// 调用初始化函数 (在应用启动时执行一次，或者通过特定脚本)
// initializeDefaultSettings().catch(err => console.error('Error initializing default settings:', err));
// 注意: 直接在此处调用可能导致每次服务器重启都尝试创建，更好的做法是放在启动脚本或迁移脚本中。
// 为了简单起见，这里仅定义，实际初始化可以通过一个一次性的脚本或手动添加。

// @route   GET /api/settings
// @desc    获取所有可编辑的系统设置 (对普通登录用户，或特定管理员)
// @access  Private (至少需要登录)
router.get('/', auth, async (req, res) => {
  try {
    // 如果需要区分管理员和普通用户能看到的设置，可以在这里加逻辑
    // 例如，只返回 isEditable: true 的设置给前端
    const settings = await Setting.find({ isEditable: true }).sort({ category: 1, name: 1 });
    res.json({ success: true, settings });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// @route   GET /api/settings/:key
// @desc    获取特定设置项 (需要管理员权限)
// @access  Private, Admin
// router.get('/:key', adminAuth, async (req, res) => { // 假设 adminAuth 中间件已存在
router.get('/:key', auth, async (req, res) => { // 暂时只用 auth，具体权限看需求
  try {
    const setting = await Setting.findOne({ key: req.params.key });
    if (!setting) {
      return res.status(404).json({ success: false, message: '设置项未找到' });
    }
    res.json({ success: true, setting });
  } catch (err) {
    console.error(err);
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// @route   PUT /api/settings/:key
// @desc    更新特定设置项 (通常需要管理员权限)
// @access  Private, Admin
// router.put('/:key', adminAuth, async (req, res) => {
router.put('/:key', auth, async (req, res) => { // 暂时只用 auth
  try {
    const { value } = req.body;
    let setting = await Setting.findOne({ key: req.params.key });

    if (!setting) {
      return res.status(404).json({ success: false, message: '设置项未找到' });
    }

    if (!setting.isEditable) {
        return res.status(403).json({ success: false, message: '此设置项不可通过UI编辑' });
    }

    setting.value = value;
    // setting.name = name || setting.name; // 如果允许修改名称等其他属性
    // setting.description = description || setting.description;
    
    const updatedSetting = await setting.save();
    res.json({ success: true, message: '设置更新成功', setting: updatedSetting });
  } catch (err) {
    console.error(err);
    if (err.name === 'ValidationError') {
        return res.status(400).json({ success: false, message: '校验失败', errors: err.errors});
    }
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});

// 示例：创建新设置项的API (通常由开发者使用或通过脚本初始化，较少暴露给前端普通管理员)
// @route   POST /api/settings
// @desc    (Admin only) 创建新设置项
// @access  Private, Admin
/*
router.post('/', adminAuth, async (req, res) => {
  try {
    const { key, value, name, description, category, isEditable } = req.body;
    let setting = await Setting.findOne({ key });
    if (setting) {
      return res.status(400).json({ success: false, message: '设置键已存在' });
    }
    setting = new Setting({ key, value, name, description, category, isEditable });
    await setting.save();
    res.status(201).json({ success: true, message: '设置项创建成功', setting });
  } catch (err) {
    console.error(err);
    if (err.name === 'ValidationError') {
        return res.status(400).json({ success: false, message: '校验失败', errors: err.errors});
    }
    res.status(500).json({ success: false, message: '服务器错误' });
  }
});
*/

module.exports = router;
module.exports.initializeDefaultSettings = initializeDefaultSettings; // 导出初始化函数，以便在app.js中调用 