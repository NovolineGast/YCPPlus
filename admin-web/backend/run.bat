@echo off
echo ========================================
echo YCPPlus Admin Backend - Quick Start
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] Cleaning old build...
call mvn clean -q

echo [2/3] Packaging application...
call mvn package -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] Build failed! Check the output above.
    pause
    exit /b 1
)

echo [3/3] Starting server...
echo.
echo Backend will start at: http://localhost:8080
echo API Endpoint: http://localhost:8080/api
echo.
echo Press Ctrl+C to stop the server
echo.
java -jar target\admin-api-1.0.0.jar

pause
