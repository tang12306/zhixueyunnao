# Education Question Bank System - Startup Guide

## Quick Start

### 1. Configure `.env`

Copy `.env.example` to `.env` in this folder and fill in at least `DB_PASSWORD`. Both Docker Compose and Spring Boot read this file (Spring Boot must be started from this folder, which `start-simple.bat` does). `.env` is ignored by Git.

| Variable | Purpose |
| --- | --- |
| `DB_PASSWORD` | Required. MySQL root password for the container and the backend |
| `DEEPSEEK_API_KEY` | Needed for AI question/exam generation |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | Creates the first administrator when the database has none (password: 8+ characters) |
| `SPRING_PROFILES_ACTIVE` | `dev` for local development (creates demo accounts); leave empty in production |

Existing database volumes keep their original root password. This change does not migrate existing database users or remove old Git history. Rotate any previously exposed passwords or API keys.

### 2. Start the System
Double-click: `start-simple.bat`

### 3. Stop the System
Double-click: `stop.bat`

## What the Startup Script Does

1. **Checks** Java and Docker, and creates `.env` from `.env.example` if it is missing (then exits so you can fill it in)
2. **Closes** service windows left over from a previous run
3. **Starts MySQL** with `docker compose up -d`
4. **Starts the Spring Boot backend** (port 8080) using `mvnw.cmd` or a system `mvn`
5. **Starts the Vue.js frontend** (port 8083)
6. **Opens the browser**

## Access Information

### Web Interface
- **Frontend**: http://localhost:8083 (proxies `/api` and `/questions` to the backend)
- **Backend / legacy pages**: http://localhost:8080

### Accounts
- **Production / default**: no built-in accounts. The first administrator comes from `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD`.
- **`dev` profile**: fictional, local-only demo accounts `demo.admin`, `demo.teacher` and students `990000001`–`990000058`. See `docs/系统账号密码说明.md`.
- The student portal is not open yet: student accounts are rejected by the frontend login.

### Database
- **MySQL**: 127.0.0.1:3306 (only reachable from this machine)

## Prerequisites

- ✅ **Docker Desktop** - Must be installed and running
- ✅ **Java 17+** - For the Spring Boot backend
- ✅ **Node.js 18+** and **npm** - For the Vue frontend
- Maven is optional; the bundled Maven Wrapper downloads it on first use

## Troubleshooting

#### 1. Docker Error
```
ERROR: Failed to start MySQL
```
**Solution**: Make sure Docker Desktop is running and `DB_PASSWORD` is set in `.env`

#### 2. Port Already in Use
**Solution**: Run `stop.bat` first

#### 3. Backend fails with "Access denied for user"
**Solution**: The MySQL volume was created with a different password. Put the original password in `DB_PASSWORD`, or reset the data with `docker compose down -v` (this deletes all data).

#### 4. Frontend shows "请先登录" on every page
**Solution**: Make sure the backend is running on port 8080, or set `VUE_APP_BACKEND_URL` in `frontend/.env.local`

### Manual Commands

```bash
# Start database
docker compose up -d

# Start backend (from this folder)
mvnw.cmd spring-boot:run

# Start frontend
cd frontend
npm install
npm run serve

# Stop database
docker compose down
```

## File Structure

```
github-upload/
├── start-simple.bat          # Startup script
├── stop.bat                  # Stop all services
├── .env.example              # Environment template
├── docker-compose.yml        # MySQL
├── frontend/                 # Vue.js frontend
└── src/                      # Spring Boot source
```

## Data Persistence

- Database data is stored in a Docker volume
- Data persists even when containers are stopped
- To completely reset: `docker compose down -v`
