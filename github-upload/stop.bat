@echo off
title Stop Educational Question Bank System

echo ========================================
echo   Stopping Educational Question Bank System
echo ========================================

set ROOT=%~dp0

echo [1/3] Stopping application windows started by start-simple.bat...
taskkill /FI "WINDOWTITLE eq Spring Boot Backend*" /T /F >nul 2>&1
if errorlevel 1 (
    echo Spring Boot window not found
) else (
    echo Spring Boot stopped
)
taskkill /FI "WINDOWTITLE eq Vue.js Frontend*" /T /F >nul 2>&1
if errorlevel 1 (
    echo Vue.js window not found
) else (
    echo Vue.js stopped
)

echo [2/3] Stopping MySQL container...
cd /d "%ROOT%"
docker compose down
if errorlevel 1 (
    echo WARNING: Failed to stop Docker services
) else (
    echo MySQL stopped
)

echo [3/3] Checking ports...
netstat -ano | findstr /R ":8080 :8083" | findstr LISTENING >nul 2>&1
if errorlevel 1 (
    echo Ports 8080 and 8083 are free
) else (
    echo WARNING: Port 8080 or 8083 is still in use by another process
)

echo ========================================
echo   System stopped.
echo   To start again, run: start-simple.bat
echo ========================================
pause
