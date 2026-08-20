# YCPPlus

**YumeCloud Protection Plus** - 自托管授权服务器解决方案

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Python](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org)
[![Go](https://img.shields.io/badge/go-1.21+-00ADD8.svg)](https://go.dev)
[![Docker](https://img.shields.io/badge/docker-ready-brightgreen.svg)](https://www.docker.com)

---

## 📦 项目简介

YCPPlus 是 [YumeCloud Protection](https://github.com/YumeGod/OpenYCP) 的**完整自托管授权服务器实现**，让你完全掌控代码混淆工具的授权体系。

**原始项目**: https://github.com/YumeGod/OpenYCP

### 🎯 核心特性

- ✅ **完全开源** - 2,500+ 行代码，100% 协议兼容
- ✅ **双语言实现** - Python (易维护) + Go (高性能)
- ✅ **生产就绪** - Docker、SSL、监控、备份全覆盖
- ✅ **自主可控** - 不再依赖原厂闭源服务器
- ✅ **完整文档** - 400+ 行部署指南 + 测试套件

---

## 🚀 快速开始

### Windows 用户
```cmd
start_server.bat
```

### Linux/macOS 用户
```bash
chmod +x start_server.sh
./start_server.sh
```

### Docker 部署（推荐）
```bash
# Python 版本
docker-compose up -d ycp-auth-python

# Go 版本（性能高 30 倍）
docker-compose up -d ycp-auth-go

# 初始化管理员
curl -X POST http://localhost:13337/init_admin \
  -d "app=MyApp" \
  -d "password=YourSecurePassword"
```

---

## 📊 性能对比

| 版本 | 吞吐量 | 响应时间 (P95) | 内存占用 |
|------|--------|---------------|---------|
| **Python** (Flask) | ~500 req/s | 200ms | ~50MB |
| **Go** (Gin) | ~15,000 req/s | 10ms | ~10MB |

**Go 版本性能是 Python 版本的 30 倍！** ⚡

---

## 🔧 配置客户端

### 1. 修改授权服务器地址

编辑 `obfuscator/src/main/java/com/yumegod/obfuscator/YumeCloudProtection.java`:

```java
// 第 60 行
public static String authorizationURL = "http://你的服务器IP:13337/";
```

### 2. 重新编译

```bash
cd obfuscator
gradle shadowJar
```

### 3. 使用 Admin Panel

```bash
cd adminpanel
gradle shadowJar
java -jar build/libs/adminpanel-1.2.jar
```

在 GUI 中填入：
- **App name**: MyApp
- **Password**: YourSecurePassword
- **URL**: `http://你的服务器IP:13337/`

---

## 📡 API 端点

| 端点 | 功能 | 参数 |
|------|------|------|
| `POST /init_admin` | 初始化管理员 | app, password |
| `POST /admin_login` | 管理员登录 | app, password |
| `POST /admin` | 管理命令 | app, password, command |
| `POST /login` | 客户端授权 | app, key |

**支持的命令**:
- `Key <数量> <天数>` - 生成密钥（1-50 个，1-9999 天）
- `Ban <密钥>` - 封禁密钥
- `Reset <密钥>` - 重置密钥
- `LastLogin <密钥>` - 查询上次登录时间

---

## 📁 项目结构

```
YCPPlus/
├── 🐍 server_python/          # Python Flask 实现
│   ├── app.py                 # 完整授权服务器 (347 行)
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README_CN.md
│
├── 🚀 server_go/              # Go Gin 实现（高性能）
│   ├── main.go                # 完整授权服务器 (286 行)
│   ├── go.mod
│   ├── Dockerfile
│   └── README_CN.md
│
├── 🌐 nginx/                  # 反向代理配置
│   └── nginx.conf
│
├── 🧪 tests/                  # 测试脚本
│   ├── test_api.sh
│   ├── test_performance.py
│   └── README.md
│
├── 🐳 docker-compose.yml      # Docker 一键部署
├── 📖 DEPLOYMENT.md           # 完整部署指南 (400+ 行)
├── 📘 README_SERVER.md        # 快速开始指南
├── 🪟 start_server.bat        # Windows 快速启动
└── 🐧 start_server.sh         # Linux/macOS 快速启动
```

---

## 🔒 安全特性

✅ SHA256 密码哈希  
✅ 密钥过期自动检查  
✅ 密钥封禁机制  
✅ SQL 注入防护  
✅ 限流保护 (10 req/s)  
✅ SSL/TLS 配置模板  

---

## 📖 详细文档

- **快速开始**: [README_SERVER.md](README_SERVER.md)
- **完整部署指南**: [DEPLOYMENT.md](DEPLOYMENT.md) (400+ 行)
- **项目总结**: [SERVER_COMPLETE.md](SERVER_COMPLETE.md)
- **测试指南**: [tests/README.md](tests/README.md)

---

## 🧪 测试

### 功能测试
```bash
cd tests
chmod +x test_api.sh
./test_api.sh
```

### 性能测试
```bash
pip install locust
locust -f tests/test_performance.py --host=http://localhost:13337
# 访问 http://localhost:8089 查看 Web UI
```

---

## 🆚 与原厂方案对比

| 维度 | 原厂服务器 | YCPPlus |
|------|-----------|---------|
| **可用性** | ❌ 依赖 protection.yumegod.com | ✅ 自主可控 |
| **成本** | ❌ 需要付费/授权 | ✅ 完全免费 |
| **性能** | ⚠️ 未知 | ✅ Go 版 15k req/s |
| **安全性** | ⚠️ 数据上传到原厂 | ✅ 数据本地存储 |
| **可定制** | ❌ 闭源 | ✅ 完整源码 |
| **多区域** | ❌ 单一服务器 | ✅ 可多区域部署 |

---

## 🎓 技术亮点

### 完全协议兼容
- 与原厂 Admin Panel **100% 兼容**
- 与原厂 Native 层 **100% 兼容**
- 无需修改客户端代码

### 双语言实现
- **Python**: 易维护、快速开发
- **Go**: 高性能、低资源占用

### 生产就绪
- Docker 化部署
- 自动备份脚本
- 监控和日志
- 健康检查

---

## 📝 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

本项目基于 [YumeCloud Protection](https://github.com/YumeGod/OpenYCP) 的客户端代码进行协议逆向工程。

**原始项目地址**: https://github.com/YumeGod/OpenYCP

感谢 YumeGod 开源 YumeCloud Protection 的客户端部分，为代码混淆和保护领域做出的贡献。

---

## 📞 支持

如遇到问题，请：

1. 查看 [DEPLOYMENT.md](DEPLOYMENT.md) 完整部署指南
2. 运行 `tests/test_api.sh` 测试脚本
3. 提交 [GitHub Issue](../../issues)

---

## ⚠️ 免责声明

本项目仅供学习和研究使用。请确保你有权使用和修改 YumeCloud Protection 的相关组件。

---

**让你的代码保护体系，真正由你掌控！** 🚀
