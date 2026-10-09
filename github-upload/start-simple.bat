@echo off
title Educational Question Bank System - Simple Startup

echo ========================================
echo   Educational Question Bank System
echo   Simple Startup Script
echo ========================================

set ROOT=%~dp0
cd /d "%ROOT%"

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

if not exist "%ROOT%.env" (
    copy "%ROOT%.env.example" "%ROOT%.env" >nul
    echo.
    echo A new .env file was created from .env.example.
    echo Fill in DB_PASSWORD ^(and DEEPSEEK_API_KEY for AI features^) in:
    echo   %ROOT%.env
    echo then run this script again.
    pause
    exit /b 1
)
echo .env: OK

echo Step 2: Stopping windows left over from a previous run...
taskkill /FI "WINDOWTITLE eq Spring Boot Backend*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq Vue.js Frontend*" /T /F >nul 2>&1
echo Cleanup: OK

echo Step 3: Starting MySQL...
docker compose up -d
if errorlevel 1 (
    echo ERROR: Failed to start MySQL. Check that DB_PASSWORD is set in .env
    pause
    exit /b 1
)

echo Step 4: Waiting for MySQL...
timeout /t 15 /nobreak >nul
echo MySQL wait: OK

echo Step 5: Starting Spring Boot backend...
where mvn >nul 2>&1
if errorlevel 1 (
    set MAVEN_CMD="%ROOT%mvnw.cmd"
) else (
    set MAVEN_CMD=mvn
)
start "Spring Boot Backend" /D "%ROOT%." cmd /k "%MAVEN_CMD% spring-boot:run"
echo Spring Boot: Starting...

echo Step 6: Starting Vue.js frontend...
start "Vue.js Frontend" /D "%ROOT%frontend" cmd /k "npm run serve"
echo Vue.js: Starting...

echo Step 7: Waiting for services to start...
echo This will take about 60-90 seconds...
timeout /t 60 /nobreak >nul

echo Step 8: Opening browser...
start http://localhost:8083

echo ========================================
echo   Startup Complete!
echo ========================================
echo Frontend: http://localhost:8083
echo Backend:  http://localhost:8080 (the frontend proxies /api to it)
echo.
echo Accounts:
echo - With SPRING_PROFILES_ACTIVE=dev in .env: demo.admin / demo.teacher,
echo   passwords are listed in the docs folder (account guide)
echo - Otherwise: APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD from .env
echo ========================================
echo.
echo IMPORTANT:
echo - Keep the service windows open
echo - If services fail to start, check the individual windows
echo - Use stop.bat to stop all services
echo ========================================
pause
