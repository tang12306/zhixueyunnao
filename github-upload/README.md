# 江苏师范大学智学云脑系统 - 教务题库系统

## 项目简介

基于 Spring Boot + Vue.js 的教育题库管理系统，支持智能出卷、题库管理、学生管理等功能。

## 系统架构

- **前端**: Vue.js 3 + Element Plus + Pinia（`frontend/`）
- **后端**: Spring Boot 3.3 + Spring Security（会话 Cookie 登录）
- **数据库**: MySQL 8（Docker）
- **AI**: DeepSeek API（密钥只从环境变量读取）

> 旧的 Node.js/Express 后端和 MongoDB 已下线并从仓库删除，所有接口都由 Spring Boot 提供。
> 学生端暂未开放：学生账号和数据保留，但前端登录只接受教师和管理员账号。

## 主要功能模块

1. **题库管理** - 题目的增删改查、分类管理
2. **智能出卷** - AI辅助生成试卷
3. **学生管理** - 学生信息管理、班级管理
4. **系统管理** - 用户权限、系统设置
5. **组织管理** - 学校、学院、专业、班级层级管理
6. **个人中心** - 用户信息管理
7. **系统设置** - 系统参数配置（仅管理员）

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Docker Desktop
- Maven 可选（仓库自带 Maven Wrapper `mvnw` / `mvnw.cmd`）

### 1. 配置 `.env`

```powershell
copy .env.example .env
```

然后编辑 `.env`（不要加引号，`.env` 已被 Git 忽略）：

| 变量 | 说明 |
| --- | --- |
| `DB_PASSWORD` | 必填。Docker MySQL 的 root 密码，后端也用它连接数据库 |
| `DB_USERNAME` | 默认 `root` |
| `DEEPSEEK_API_KEY` | 使用 AI 出题时填写 |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | 数据库里还没有管理员时，启动时用它创建首个管理员（密码至少 8 位） |
| `SPRING_PROFILES_ACTIVE` | 本地开发填 `dev`，会创建演示账号；生产环境留空 |
| `COOKIE_SECURE` | 上线 HTTPS 后设为 `true` |
| `CORS_ALLOWED_ORIGINS` | 前端与后端不同源时才需要，逗号分隔 |

Docker Compose 和 Spring Boot 都会读取这个 `.env`（Spring Boot 需从 `github-upload` 目录启动）。已有数据库卷会保留原来的 root 密码，修改 `.env` 不会改变它。

### 2. 启动

```bash
# Windows：一键启动（MySQL + Spring Boot + Vue）
start-simple.bat

# 或手动启动
docker compose up -d
mvnw.cmd spring-boot:run          # macOS/Linux: ./mvnw spring-boot:run
cd frontend && npm install && npm run serve
```

停止：`stop.bat`。

表结构由 Flyway 管理（`src/main/resources/db/migration`），后端启动时自动执行还没执行过的脚本，Hibernate 只校验实体和表是否一致。以前靠 `ddl-auto=update` 建好的库，首次启动会自动记为 V1 基线，再执行后面的脚本。修改表结构时新增 `V<序号>__说明.sql`，不要改已经发布的脚本。

### 3. 访问

- 前端: http://localhost:8083（开发服务器把 `/api`、`/questions` 代理到后端，生产环境由 Nginx 做同样的转发）
- 后端: http://localhost:8080（旧版 Thymeleaf 页面也在这里）

### 账号

- **生产 / 默认**：没有任何内置账号。首个管理员由 `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` 创建。
- **dev 配置**（`SPRING_PROFILES_ACTIVE=dev`）：额外创建虚构的演示管理员 `demo.admin / DemoAdmin123!`、演示教师 `demo.teacher / DemoTeacher123!` 和58个虚构学生 `990000001` ~ `990000058`（姓名为 `示例学生001` ~ `示例学生058`，初始密码等于虚构学号）。这些密码是公开的，只能用于本地演示。

详见 [系统账号说明](docs/系统账号密码说明.md)。

### 隐私说明

- 当前源码和文档使用虚构姓名、账号和学号，不提交真实学生名单或个人登录凭据。
- 环境变量只控制新启动的服务，代码替换不会删除或迁移已部署数据库中的既有用户。
- 普通提交不会清除旧 Git 历史；已泄露的密码和 API 密钥应立即更换，历史清理需单独处理。
- 可运行 `node scripts/check-public-data.cjs` 检查示例数据和敏感凭据是否符合公开仓库规范。

## 测试与 CI

```bash
mvnw.cmd verify                   # 后端测试，使用 H2 内存库，不需要 MySQL
cd frontend && npm run build      # 前端构建
node scripts/check-public-data.cjs
```

GitHub Actions（仓库根目录 `.github/workflows/ci.yml`）在每次推送和 PR 时运行这三项。

## 项目结构

```
├── src/                   # Spring Boot 源码（数据库迁移脚本在 src/main/resources/db/migration）
├── frontend/              # Vue.js 前端
├── docs/                  # 项目文档
├── scripts/               # 检查脚本
├── .env.example           # 环境变量模板
├── start-simple.bat       # 启动脚本
├── stop.bat               # 停止脚本
├── docker-compose.yml     # Docker 编排（仅 MySQL）
└── pom.xml                # Maven 配置
```

## 开发说明

详细的开发文档请参考 `docs/` 目录下的相关文档：

- [系统启动说明](README-Startup.md)
- [数据库设计](docs/数据库表结构详细设计.md)
- [系统账号说明](docs/系统账号密码说明.md)

`docs/` 下其余的“修复说明”类文档是历史记录，其中提到的 Node.js 后端、匿名接口等已不再适用。

## 许可证

本项目仅供学习和研究使用。

## 联系方式

如有问题请查看文档或提交Issue。
