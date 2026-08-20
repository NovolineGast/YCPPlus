@echo off
chcp 65001 >nul
echo ==========================================
echo YumeCloud Protection 授权服务器快速启动
echo ==========================================
echo.

echo 请选择部署方式:
echo 1) Python 版本（推荐测试和开发）
echo 2) Go 版本（推荐生产环境，性能高 30 倍）
echo 3) Docker 部署（最简单）
echo.
set /p choice="请输入选项 [1-3]: "

if "%choice%"=="1" goto python
if "%choice%"=="2" goto golang
if "%choice%"=="3" goto docker
goto invalid

:python
echo.
echo === 部署 Python 版本 ===
cd server_python

REM 检查 Python
where python >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到 Python，请先安装
    pause
    exit /b 1
)

REM 安装依赖
echo 安装依赖...
pip install -r requirements.txt

REM 启动服务器
echo.
echo 启动授权服务器...
start /B python app.py

timeout /t 3 >nul

REM 初始化管理员
echo.
echo === 初始化管理员账号 ===
set /p APP_NAME="应用名称 (App Name): "
set /p PASSWORD="密码 (Password): "

curl -X POST http://localhost:13337/init_admin -d "app=%APP_NAME%" -d "password=%PASSWORD%"

echo.
echo.
echo ✅ Python 版本部署完成！
echo 服务器运行在: http://localhost:13337
echo.
echo 停止服务器: 在任务管理器中结束 python.exe 进程
goto end

:golang
echo.
echo === 部署 Go 版本 ===
cd server_go

REM 检查 Go
where go >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到 Go，请先安装 https://go.dev/dl/
    pause
    exit /b 1
)

REM 下载依赖
echo 下载依赖...
go mod download

REM 编译
echo 编译...
go build -o ycp-auth.exe main.go

REM 启动服务器
echo.
echo 启动授权服务器...
start /B ycp-auth.exe

timeout /t 2 >nul

REM 初始化管理员
echo.
echo === 初始化管理员账号 ===
set /p APP_NAME="应用名称 (App Name): "
set /p PASSWORD="密码 (Password): "

curl -X POST http://localhost:13337/init_admin -d "app=%APP_NAME%" -d "password=%PASSWORD%"

echo.
echo.
echo ✅ Go 版本部署完成！
echo 服务器运行在: http://localhost:13337
echo 可执行文件: server_go\ycp-auth.exe
echo.
echo 停止服务器: 在任务管理器中结束 ycp-auth.exe 进程
goto end

:docker
echo.
echo === Docker 部署 ===

REM 检查 Docker
where docker >nul 2>nul
if %errorlevel% neq 0 (
    echo 错误: 未找到 Docker，请先安装
    pause
    exit /b 1
)

REM 选择版本
echo.
echo 选择版本:
echo 1) Python 版本
echo 2) Go 版本
set /p docker_choice="请输入选项 [1-2]: "

if "%docker_choice%"=="1" (
    set SERVICE=ycp-auth-python
) else (
    set SERVICE=ycp-auth-go
)

REM 启动容器
echo.
echo 启动 Docker 容器...
docker-compose up -d %SERVICE%

timeout /t 5 >nul

REM 初始化管理员
echo.
echo === 初始化管理员账号 ===
set /p APP_NAME="应用名称 (App Name): "
set /p PASSWORD="密码 (Password): "

docker exec %SERVICE% curl -X POST http://localhost:13337/init_admin -d "app=%APP_NAME%" -d "password=%PASSWORD%"

echo.
echo.
echo ✅ Docker 部署完成！
echo 服务器运行在: http://localhost:13337
echo 容器名称: %SERVICE%
echo.
echo 查看日志: docker logs %SERVICE%
echo 停止服务器: docker-compose down
goto end

:invalid
echo 无效选项
pause
exit /b 1

:end
echo.
echo ==========================================
echo 下一步操作:
echo ==========================================
echo.
echo 1. 修改 YumeCloudProtection 配置:
echo    文件: obfuscator\src\main\java\com\yumegod\obfuscator\YumeCloudProtection.java
echo    第 60 行: authorizationURL = "http://你的服务器IP:13337/"
echo.
echo 2. 重新编译 obfuscator:
echo    cd obfuscator ^&^& gradle shadowJar
echo.
echo 3. 启动 Admin Panel 管理密钥:
echo    cd adminpanel ^&^& gradle shadowJar
echo    java -jar build\libs\adminpanel-1.2.jar
echo.
echo 4. 在 Admin Panel 中填入:
echo    App name: %APP_NAME%
echo    Password: ******
echo    URL: http://localhost:13337/
echo.
echo 详细文档: README_SERVER.md
echo ==========================================
echo.
pause
