#!/bin/bash

echo "========================================"
echo "YCPPlus 服务状态检查"
echo "========================================"
echo ""

# 检查后端
echo "🔍 检查后端 (http://localhost:8080)..."
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1 || curl -s http://localhost:8080/api/dashboard/stats > /dev/null 2>&1; then
    echo "✅ 后端运行中"
    echo "   API: http://localhost:8080/api"
else
    echo "❌ 后端未启动"
fi

echo ""

# 检查前端
echo "🔍 检查前端 (http://localhost:5173)..."
if curl -s http://localhost:5173 > /dev/null 2>&1; then
    echo "✅ 前端运行中"
    echo "   URL: http://localhost:5173"
else
    echo "❌ 前端未启动"
fi

echo ""
echo "========================================"
echo "测试 API 端点:"
echo "========================================"
echo ""

# 测试统计接口
echo "📊 Dashboard Stats:"
curl -s http://localhost:8080/api/dashboard/stats 2>/dev/null | head -3

echo ""
echo ""
echo "✨ 访问管理面板: http://localhost:5173"
echo ""
