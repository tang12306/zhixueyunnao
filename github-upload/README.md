# 江苏师范大学智学云脑系统 - 教务题库系统

## 项目简介

这是一个基于Spring Boot + Vue.js + Node.js的教育题库管理系统，支持智能出卷、题库管理、学生管理等功能。

## 系统架构

- **前端**: Vue.js 3 + Element Plus
- **后端**: Spring Boot 2.7 + Node.js Express
- **数据库**: MySQL + MongoDB
- **容器化**: Docker + Docker Compose

## 主要功能模块

1. **题库管理** - 题目的增删改查、分类管理
2. **智能出卷** - AI辅助生成试卷
3. **学生管理** - 学生信息管理、班级管理
4. **系统管理** - 用户权限、系统设置
5. **组织管理** - 学校、学院、专业、班级层级管理
6. **个人中心** - 用户信息管理
7. **系统设置** - 系统参数配置

## 快速开始

### 环境要求

- Java 17+
- Node.js 16+
- Docker Desktop
- Maven 3.6+

### 启动步骤

1. 克隆项目
```bash
git clone [repository-url]
cd 教务题库系统
```

2. 配置本地环境变量

数据库密码和 API 密钥不再硬编码。Windows PowerShell 示例：

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '填写你自己的本地数据库密码'
# 如需 AI 功能，请填写从服务商获取的新密钥；不要提交到 Git
$env:DEEPSEEK_API_KEY = '填写你自己的 API 密钥'
# 仅在使用 src/main/application.yml 时需要独立的 JWT_SECRET
$env:JWT_SECRET = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
.\start-simple.bat
```

`DB_PASSWORD` 同时用于 Docker MySQL root 用户和 Spring Boot 默认数据库连接。已有数据库卷请填写原先配置的数据库密码，修改环境变量不会自动更改现有数据库密码。若使用其他数据库用户，请相应设置 `DB_USERNAME` 和该用户的密码。`.env` 文件可供 Docker Compose 使用，但 Spring Boot 不会自动加载它；从已设置环境变量的同一终端启动 Java 服务。

3. 启动系统
```bash
# Windows
start-simple.bat

# 或手动启动各服务
docker-compose up -d
cd backend && npm install && npm start
cd frontend && npm install && npm run serve
mvn spring-boot:run
```

4. 访问系统
- 前端地址: http://localhost:8083
- 后端API: http://localhost:8080
- Node.js API: http://localhost:5000

### 默认账号

- 用户名: admin
- 密码: admin123

Spring Boot 初始化器还提供虚构教师 `demo.teacher / DemoTeacher123!` 和58个虚构学生 `990000001` ~ `990000058`（姓名为 `示例学生001` ~ `示例学生058`，初始密码等于虚构学号）。这些数据仅用于本地演示，禁止作为生产账号使用。

### 隐私说明

- 当前源码和文档使用虚构姓名、账号和学号，不提交真实学生名单或个人登录凭据。
- 环境变量只控制新启动的服务，代码替换不会删除或迁移已部署数据库中的既有用户。
- 普通提交不会清除旧 Git 历史；已泄露的密码和 API 密钥应立即更换，历史清理需单独处理。
- 可运行 `node scripts/check-public-data.cjs` 检查示例数据和敏感凭据是否符合公开仓库规范。

## 项目结构

```
├── src/                    # Spring Boot源码
├── frontend/              # Vue.js前端
├── backend/               # Node.js后端
├── docker/                # Docker配置
├── docs/                  # 项目文档
├── start-simple.bat       # 启动脚本
├── stop.bat              # 停止脚本
├── docker-compose.yml    # Docker编排
└── pom.xml               # Maven配置
```

## 开发说明

详细的开发文档请参考 `docs/` 目录下的相关文档：

- [系统启动说明](README-Startup.md)
- [项目主文档](docs/项目主README.md)
- [数据库设计](docs/数据库表结构详细设计.md)
- [系统账号说明](docs/系统账号密码说明.md)

## 技术特色

- 前后端分离架构
- 微服务设计理念
- Docker容器化部署
- AI智能出卷功能
- 响应式界面设计
- 完整的权限管理

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题请查看文档或提交Issue。
