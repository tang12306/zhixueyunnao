@echo off
title Stop Educational Question Bank System
chcp 65001 >nul

echo ========================================
echo   Stopping Educational Question Bank System
echo ========================================

set ROOT=%~dp0
set LOG_FILE=%ROOT%startup.log

echo [1/4] Stopping application services...
echo Stopping Java processes (Spring Boot)...
taskkill /F /IM java.exe >nul 2>&1
if errorlevel 1 (
    echo No Java processes found
) else (
    echo ✓ Java processes stopped
)

echo Stopping Node.js processes...
taskkill /F /IM node.exe >nul 2>&1
if errorlevel 1 (
    echo No Node.js processes found
) else (
    echo ✓ Node.js processes stopped
)

echo [2/4] Stopping Docker services...
cd /d "%ROOT%"
docker-compose down
if errorlevel 1 (
    echo WARNING: Failed to stop Docker services
) else (
    echo ✓ Docker services stopped
)

echo [3/4] Cleaning up ports...
echo Checking port usage...
netstat -ano | findstr ":5000\|:8080\|:8083" >nul 2>&1
if errorlevel 1 (
    echo ✓ All ports are free
) else (
    echo WARNING: Some ports may still be in use
    echo You may need to restart your computer if issues persist
)

echo [4/4] Cleanup completed
echo [%date% %time%] System stopped >> "%LOG_FILE%"

echo ========================================
echo   🛑 System Stopped Successfully!
echo ========================================
echo All services have been terminated.
echo Docker containers have been stopped.
echo 
echo To restart the system, run: start.bat
echo ========================================
pause
