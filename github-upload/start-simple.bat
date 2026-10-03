@echo off
title Educational Question Bank System - Simple Startup

echo ========================================
echo   Educational Question Bank System
echo   Simple Startup Script
echo ========================================

set ROOT=%~dp0

echo Step 1: Checking environment...
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java not found. Please install Java 17 or higher.
    pause
    exit /b 1
)
echo Java: OK

docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker not found. Please install Docker Desktop.
    pause
    exit /b 1
)
echo Docker: OK

echo Step 2: Cleaning existing processes...
taskkill /F /IM java.exe >nul 2>&1
taskkill /F /IM node.exe >nul 2>&1
echo Process cleanup: OK

echo Step 3: Starting databases...
cd /d "%ROOT%"
docker-compose up -d
if errorlevel 1 (
    echo ERROR: Failed to start databases
    pause
    exit /b 1
)
echo Databases: Starting...

echo Step 4: Waiting for databases...
timeout /t 15 /nobreak >nul
echo Database wait: OK

echo Step 5: Starting Node.js backend...
cd /d "%ROOT%\backend"
start "Node.js Backend" cmd /k "echo Starting Node.js Backend... && npm start"
echo Node.js: Starting...

echo Step 6: Starting Spring Boot backend...
cd /d "%ROOT%"
where mvn >nul 2>&1
if errorlevel 1 (
    set MAVEN_CMD=%ROOT%mvnw.cmd
) else (
    set MAVEN_CMD=mvn
)
start "Spring Boot Backend" cmd /k "echo Starting Spring Boot Backend... && echo Current directory: %CD% && %MAVEN_CMD% spring-boot:run"
echo Spring Boot: Starting...

echo Step 7: Starting Vue.js frontend...
cd /d "%ROOT%\frontend"
start "Vue.js Frontend" cmd /k "echo Starting Vue.js Frontend... && npm run serve"
echo Vue.js: Starting...

echo Step 8: Waiting for services to start...
echo This will take about 60-90 seconds...
timeout /t 60 /nobreak >nul

echo Step 9: Opening browser...
start http://localhost:8083

echo ========================================
echo   Startup Complete!
echo ========================================
echo Frontend: http://localhost:8083
echo Node.js API: http://localhost:5000
echo Spring Boot API: http://localhost:8080
echo Login: admin / admin123
echo ========================================
echo.
echo IMPORTANT: 
echo - Keep the service windows open
echo - If services fail to start, check the individual windows
echo - Use stop.bat to stop all services
echo ========================================
pause
