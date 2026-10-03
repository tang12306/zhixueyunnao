const jwt = require('jsonwebtoken');

module.exports = (req, res, next) => {
  // 从头部获取token
  const token = req.header('x-auth-token');

  // 检查是否有token
  if (!token) {
    return res.status(401).json({ 
      success: false, 
      message: '无访问权限，请提供有效的令牌' 
    });
  }

  try {
    // 验证token
    const decoded = jwt.verify(token, process.env.JWT_SECRET || 'your_jwt_secret');
    
    // 将用户信息添加到请求中
    req.user = decoded;
    next();
  } catch (err) {
    res.status(401).json({ 
      success: false, 
      message: '无效的令牌'
    });
  }
}; 