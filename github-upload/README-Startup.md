# 🚀 Education Question Bank System - Startup Guide

## Quick Start

### 1. Configure Local Credentials

The repository does not contain real student identities or private database/API credentials. Before starting services, set `DB_PASSWORD` to your local MySQL root password in the same terminal that launches the startup script. Set `DB_USERNAME` if you use a different database user, and `DEEPSEEK_API_KEY` if you need AI features. The alternate YAML configuration also requires a separately generated `JWT_SECRET`. Docker Compose can load a local `.env`, but Spring Boot does not load that file automatically. Existing database volumes retain their original credentials.

All `demo.teacher` and `990000001`–`990000058` accounts are fictional, local-only examples. This change does not migrate existing database users or remove old Git history. Rotate any previously exposed passwords or API keys.

### 2. Start the System
Double-click: `一键启动项目.bat`

### 3. Stop the System
Double-click: `stop-services.bat`

## What the Startup Script Does

1. **Starts Database Containers** (MongoDB + MySQL)
2. **Installs Dependencies** (if not already installed)
3. **Starts Node.js Backend** (port 5000)
4. **Starts Vue.js Frontend** (port 8083)
5. **Starts Spring Boot Backend** (port 8080, optional)
6. **Creates Default Admin User**
7. **Opens Browser** automatically

## Access Information

### Web Interface
- **Frontend**: http://localhost:8083
- **Default Login**: admin / admin123

### API Endpoints
- **Node.js API**: http://localhost:5000
- **Spring Boot API**: http://localhost:8080

### Database
- **MongoDB**: localhost:27017
- **MySQL**: localhost:3306

## Prerequisites

### Required
- ✅ **Docker Desktop** - Must be installed and running
- ✅ **Node.js** - Version 16 or higher
- ✅ **npm** - Comes with Node.js

### Optional
- ⚠️ **Maven** - For Spring Boot backend

## Troubleshooting

### Common Issues

#### 1. Docker Error
```
ERROR: Failed to start database containers
```
**Solution**: Make sure Docker Desktop is running

#### 2. Port Already in Use
**Solution**: Run `stop-services.bat` first

#### 3. Dependencies Installation Failed
**Solution**: 
- Check internet connection
- Clear npm cache: `npm cache clean --force`
- Delete node_modules folders and retry

#### 4. Services Not Starting
**Solution**:
- Check the service windows for error messages
- Make sure all prerequisites are installed
- Restart Docker Desktop

### Manual Commands

If the script fails, you can run commands manually:

```bash
# Start database
docker-compose up -d

# Start backend
cd backend
npm install
npm start

# Start frontend  
cd frontend
npm install
npm run serve

# Stop everything
docker-compose down
```

## File Structure

```
教务题库系统/
├── 一键启动项目.bat          # Main startup script
├── stop-services.bat         # Stop all services
├── docker-compose.yml        # Database configuration
├── backend/                  # Node.js backend
├── frontend/                 # Vue.js frontend
└── src/                      # Spring Boot source
```

## Success Indicators

When everything is working correctly:
- ✅ 3 command windows open (database, backend, frontend)
- ✅ Browser opens to http://localhost:8083
- ✅ Can login with admin/admin123
- ✅ All services respond properly

## Data Persistence

- Database data is stored in Docker volumes
- Data persists even when containers are stopped
- To completely reset: `docker-compose down -v`

## Support

If you encounter issues:
1. Check the service windows for error messages
2. Ensure all prerequisites are installed
3. Try running `stop-services.bat` then restart
4. Check Docker Desktop is running properly

---

**Remember**: The startup script is now in English to avoid character encoding issues! 🚀
