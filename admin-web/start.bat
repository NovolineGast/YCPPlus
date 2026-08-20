@echo off
echo ================================================
echo YCPPlus Admin Web - Quick Start
echo ================================================
echo.

cd /d "%~dp0"

echo [1/3] Starting Backend...
start "YCP Backend" cmd /k "cd backend && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo [2/3] Installing Frontend Dependencies...
cd frontend
if not exist node_modules (
    call npm install
)

echo [3/3] Starting Frontend...
start "YCP Frontend" cmd /k "npm run dev"

echo.
echo ================================================
echo Services Starting...
echo ================================================
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo.
echo Open your browser and visit http://localhost:5173
echo ================================================
