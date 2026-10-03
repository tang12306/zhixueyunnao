# 教务题库管理系统

这是一个完整的教务题库管理系统，支持以下功能：
- 手动录入题库
- 从Excel自动导入题库
- 依照题目类型自动出题
- 集成DeepSeek AI接口辅助教师出题
- 支持语文、数学、英语三门学科

## 技术栈
- 前端：Vue.js + Element Plus
- 后端：Node.js + Express
- 数据库：MongoDB
- AI接口：DeepSeek API

## 安装与运行
1. 克隆项目
2. 安装依赖
   ```
   cd frontend && npm install
   cd ../backend && npm install
   ```
3. 启动项目
   ```
   # 前端
   cd frontend && npm run serve
   # 后端
   cd backend && npm start
   ```

## 项目结构
- frontend/：前端代码
- backend/：后端代码
  - models/：数据库模型
  - routes/：API路由
  - controllers/：业务逻辑
  - services/：服务层
  - utils/：工具函数 