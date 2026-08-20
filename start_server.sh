#!/bin/bash

echo "=========================================="
echo "YumeCloud Protection 授权服务器快速启动"
echo "=========================================="
echo ""

# 检测操作系统
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="Linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macOS"
else
    OS="Unknown"
fi

echo "检测到操作系统: $OS"
echo ""

# 选择部署方式
echo "请选择部署方式:"
echo "1) Python 版本（推荐测试和开发）"
echo "2) Go 版本（推荐生产环境，性能高 30 倍）"
echo "3) Docker 部署（最简单）"
echo ""
read -p "请输入选项 [1-3]: " choice

case $choice in
    1)
        echo ""
        echo "=== 部署 Python 版本 ==="
        cd server_python || exit

        # 检查 Python
        if ! command -v python3 &> /dev/null; then
            echo "错误: 未找到 Python3，请先安装"
            exit 1
        fi

        # 安装依赖
        echo "安装依赖..."
        pip3 install -r requirements.txt

        # 启动服务器
        echo ""
        echo "启动授权服务器..."
        python3 app.py &
        SERVER_PID=$!

        sleep 3

        # 初始化管理员
        echo ""
        echo "=== 初始化管理员账号 ==="
        read -p "应用名称 (App Name): " APP_NAME
        read -sp "密码 (Password): " PASSWORD
        echo ""

        curl -X POST http://localhost:13337/init_admin \
            -d "app=$APP_NAME" \
            -d "password=$PASSWORD"

        echo ""
        echo ""
        echo "✅ Python 版本部署完成！"
        echo "服务器运行在: http://localhost:13337"
        echo "进程 PID: $SERVER_PID"
        echo ""
        echo "停止服务器: kill $SERVER_PID"
        ;;

    2)
        echo ""
        echo "=== 部署 Go 版本 ==="
        cd server_go || exit

        # 检查 Go
        if ! command -v go &> /dev/null; then
            echo "错误: 未找到 Go，请先安装 https://go.dev/dl/"
            exit 1
        fi

        # 下载依赖
        echo "下载依赖..."
        go mod download

        # 编译
        echo "编译..."
        go build -o ycp-auth main.go

        # 启动服务器
        echo ""
        echo "启动授权服务器..."
        ./ycp-auth &
        SERVER_PID=$!

        sleep 2

        # 初始化管理员
        echo ""
        echo "=== 初始化管理员账号 ==="
        read -p "应用名称 (App Name): " APP_NAME
        read -sp "密码 (Password): " PASSWORD
        echo ""

        curl -X POST http://localhost:13337/init_admin \
            -d "app=$APP_NAME" \
            -d "password=$PASSWORD"

        echo ""
        echo ""
        echo "✅ Go 版本部署完成！"
        echo "服务器运行在: http://localhost:13337"
        echo "进程 PID: $SERVER_PID"
        echo "可执行文件: ./server_go/ycp-auth"
        echo ""
        echo "停止服务器: kill $SERVER_PID"
        ;;

    3)
        echo ""
        echo "=== Docker 部署 ==="

        # 检查 Docker
        if ! command -v docker &> /dev/null; then
            echo "错误: 未找到 Docker，请先安装"
            exit 1
        fi

        # 选择版本
        echo ""
        echo "选择版本:"
        echo "1) Python 版本"
        echo "2) Go 版本"
        read -p "请输入选项 [1-2]: " docker_choice

        if [ "$docker_choice" == "1" ]; then
            SERVICE="ycp-auth-python"
        else
            SERVICE="ycp-auth-go"
        fi

        # 启动容器
        echo ""
        echo "启动 Docker 容器..."
        docker-compose up -d $SERVICE

        sleep 5

        # 初始化管理员
        echo ""
        echo "=== 初始化管理员账号 ==="
        read -p "应用名称 (App Name): " APP_NAME
        read -sp "密码 (Password): " PASSWORD
        echo ""

        docker exec $SERVICE curl -X POST http://localhost:13337/init_admin \
            -d "app=$APP_NAME" \
            -d "password=$PASSWORD"

        echo ""
        echo ""
        echo "✅ Docker 部署完成！"
        echo "服务器运行在: http://localhost:13337"
        echo "容器名称: $SERVICE"
        echo ""
        echo "查看日志: docker logs $SERVICE"
        echo "停止服务器: docker-compose down"
        ;;

    *)
        echo "无效选项"
        exit 1
        ;;
esac

echo ""
echo "=========================================="
echo "下一步操作:"
echo "=========================================="
echo ""
echo "1. 修改 YumeCloudProtection 配置:"
echo "   文件: obfuscator/src/main/java/com/yumegod/obfuscator/YumeCloudProtection.java"
echo "   第 60 行: authorizationURL = \"http://你的服务器IP:13337/\""
echo ""
echo "2. 重新编译 obfuscator:"
echo "   cd obfuscator && gradle shadowJar"
echo ""
echo "3. 启动 Admin Panel 管理密钥:"
echo "   cd adminpanel && gradle shadowJar"
echo "   java -jar build/libs/adminpanel-1.2.jar"
echo ""
echo "4. 在 Admin Panel 中填入:"
echo "   App name: $APP_NAME"
echo "   Password: ******"
echo "   URL: http://localhost:13337/"
echo ""
echo "详细文档: README_SERVER.md"
echo "=========================================="
