# 测试脚本说明

本目录包含用于测试授权服务器的脚本。

## 测试脚本列表

### 1. test_api.sh (Linux/macOS)
测试所有 API 端点的功能

```bash
chmod +x test_api.sh
./test_api.sh
```

### 2. test_api.bat (Windows)
Windows 版本的测试脚本

```cmd
test_api.bat
```

### 3. test_performance.py
性能测试脚本（使用 locust）

```bash
pip install locust
locust -f test_performance.py --host=http://localhost:13337
```

## 测试内容

1. **初始化管理员** - `/init_admin`
2. **管理员登录** - `/admin_login`
3. **生成密钥** - `/admin` with `Key` command
4. **客户端授权** - `/login`
5. **封禁密钥** - `/admin` with `Ban` command
6. **重置密钥** - `/admin` with `Reset` command
7. **查询登录记录** - `/admin` with `LastLogin` command

## 预期结果

所有测试应该返回成功状态，性能测试应该显示：
- Python 版本: ~500 req/s
- Go 版本: ~15,000 req/s
