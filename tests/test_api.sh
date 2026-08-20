#!/bin/bash

# YumeCloud Protection 授权服务器 API 测试脚本

BASE_URL="http://localhost:13337"
APP_NAME="TestApp"
PASSWORD="TestPassword123"
TEST_KEY=""

echo "=========================================="
echo "YumeCloud Protection API 测试"
echo "=========================================="
echo ""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试函数
test_api() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected=$5

    echo -n "测试: $test_name ... "

    response=$(curl -s -X $method "$BASE_URL$endpoint" $data)

    if [[ "$response" == *"$expected"* ]]; then
        echo -e "${GREEN}✓ 通过${NC}"
        return 0
    else
        echo -e "${RED}✗ 失败${NC}"
        echo "  预期: $expected"
        echo "  实际: $response"
        return 1
    fi
}

echo "1️⃣  测试: 初始化管理员"
response=$(curl -s -X POST "$BASE_URL/init_admin" \
    -d "app=$APP_NAME" \
    -d "password=$PASSWORD")

if [[ "$response" == *"Admin created successfully"* ]] || [[ "$response" == *"App already exists"* ]]; then
    echo -e "${GREEN}✓ 通过${NC} ($response)"
else
    echo -e "${RED}✗ 失败${NC} ($response)"
fi
echo ""

echo "2️⃣  测试: 管理员登录"
test_api "正确凭据登录" "POST" "/admin_login" \
    "-d 'app=$APP_NAME' -d 'password=$PASSWORD'" \
    "success"
echo ""

test_api "错误密码登录" "POST" "/admin_login" \
    "-d 'app=$APP_NAME' -d 'password=WrongPassword'" \
    "Invalid"
echo ""

echo "3️⃣  测试: 生成密钥"
response=$(curl -s -X POST "$BASE_URL/admin" \
    -d "app=$APP_NAME" \
    -d "password=$PASSWORD" \
    -d "command=Key 5 30")

if [[ "$response" == "$APP_NAME"* ]]; then
    echo -e "${GREEN}✓ 通过${NC}"
    echo "生成的密钥:"
    echo "$response" | head -5
    # 保存第一个密钥用于后续测试
    TEST_KEY=$(echo "$response" | head -1)
    echo ""
    echo "测试密钥: $TEST_KEY"
else
    echo -e "${RED}✗ 失败${NC}"
    echo "响应: $response"
fi
echo ""

if [ -z "$TEST_KEY" ]; then
    echo -e "${YELLOW}警告: 无法获取测试密钥，跳过后续测试${NC}"
    exit 1
fi

echo "4️⃣  测试: 客户端授权验证"
test_api "有效密钥验证" "POST" "/login" \
    "-d 'app=$APP_NAME' -d 'key=$TEST_KEY'" \
    "success"
echo ""

test_api "无效密钥验证" "POST" "/login" \
    "-d 'app=$APP_NAME' -d 'key=InvalidKey'" \
    "Invalid key"
echo ""

echo "5️⃣  测试: 查询上次登录时间"
response=$(curl -s -X POST "$BASE_URL/admin" \
    -d "app=$APP_NAME" \
    -d "password=$PASSWORD" \
    -d "command=LastLogin $TEST_KEY")

if [[ "$response" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2} ]] || [[ "$response" == "null" ]]; then
    echo -e "${GREEN}✓ 通过${NC}"
    echo "上次登录时间: $response"
else
    echo -e "${RED}✗ 失败${NC}"
    echo "响应: $response"
fi
echo ""

echo "6️⃣  测试: 重置密钥"
test_api "重置密钥登录记录" "POST" "/admin" \
    "-d 'app=$APP_NAME' -d 'password=$PASSWORD' -d 'command=Reset $TEST_KEY'" \
    "success"
echo ""

echo "7️⃣  测试: 封禁密钥"
test_api "封禁指定密钥" "POST" "/admin" \
    "-d 'app=$APP_NAME' -d 'password=$PASSWORD' -d 'command=Ban $TEST_KEY'" \
    "success"
echo ""

echo "8️⃣  测试: 被封禁密钥验证"
test_api "已封禁密钥应该失败" "POST" "/login" \
    "-d 'app=$APP_NAME' -d 'key=$TEST_KEY'" \
    "Key banned"
echo ""

echo "=========================================="
echo "测试完成！"
echo "=========================================="
echo ""

# 性能测试提示
echo -e "${YELLOW}性能测试:${NC}"
echo "使用 Apache Bench:"
echo "  ab -n 1000 -c 10 -p login.txt -T 'application/x-www-form-urlencoded' $BASE_URL/login"
echo ""
echo "使用 Locust:"
echo "  pip install locust"
echo "  locust -f test_performance.py --host=$BASE_URL"
