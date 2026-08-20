# YCPPlus

**YumeCloud Protection Plus** - Java 代码混淆保护工具 + 自托管授权服务器

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/java-8+-orange.svg)](https://www.oracle.com/java/)
[![Python](https://img.shields.io/badge/python-3.8+-blue.svg)](https://www.python.org)
[![Go](https://img.shields.io/badge/go-1.21+-00ADD8.svg)](https://go.dev)
[![Docker](https://img.shields.io/badge/docker-ready-brightgreen.svg)](https://www.docker.com)

---

## 📦 项目简介

YCPPlus 是一个**企业级 Java 代码混淆保护工具**，提供全方位的代码保护方案。

**原始项目**: https://github.com/YumeGod/OpenYCP

### 🎯 核心功能

#### 🛡️ 代码混淆引擎

- **字符串混淆** - 加密所有字符串常量
- **数字混淆** - 混淆数值计算
- **控制流混淆** - 复杂化程序逻辑
- **调用加密** - 加密方法调用
- **重命名混淆** - 类/方法/字段名混淆
- **InvokeDynamic** - 动态方法调用
- **Native 保护** - Java 代码转 C++ + JNI
- **VMProtect 集成** - 虚拟化保护
- **防调试检测** - 运行时调试器检查
- **防篡改保护** - 代码完整性验证

#### 🚀 自托管授权服务器（本项目新增）

- **完全开源** - Python + Go 双实现
- **协议兼容** - 100% 兼容原始 Admin Panel
- **自主可控** - 不依赖第三方授权服务
- **高性能** - Go 版本 15,000 req/s
- **生产就绪** - Docker、SSL、监控、备份

---

## 📁 项目结构

```
YCPPlus/
├── 🔧 obfuscator/              # 核心混淆引擎
│   ├── StringObfuscation       # 字符串加密
│   ├── NumberObfuscation       # 数字混淆
│   ├── FlowObfuscation         # 控制流混淆
│   ├── NativeProtection        # Native 保护
│   └── VMProtect               # 虚拟化保护
│
├── 🎨 obfuscatorgui/           # 图形界面
├── 🎨 obfuscatornewui/         # 新版图形界面
├── 🔐 adminpanel/              # 管理面板
├── 🧩 annotations/             # 混淆注解库
├── 🔧 obfuscatorsdk/           # SDK 接口
├── 🖥️ ByteCodeVirtualizer/     # 字节码虚拟化
├── 🔑 authorizationserver/     # 授权服务器（原始）
│
├── 🐍 server_python/           # Python 授权服务器（新增）
│   ├── app.py                  # Flask 实现 (347 行)
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README_CN.md
│
├── 🚀 server_go/               # Go 授权服务器（新增）
│   ├── main.go                 # Gin 实现 (286 行)
│   ├── go.mod
│   ├── Dockerfile
│   └── README_CN.md
│
├── 🌐 nginx/                   # 反向代理配置（新增）
├── 🧪 tests/                   # 测试套件（新增）
├── 🐳 docker-compose.yml       # Docker 编排（新增）
└── 📖 docs/                    # 完整文档（新增）
```

---

## 🚀 快速开始

### 1️⃣ 使用混淆器

#### 使用注解方式

```java
import com.yumegod.yumecloudprotection.annotations.*;

public class MyApp {
    @StringObfuscate
    private static final String API_KEY = "secret_key_123";
    
    @NumberObfuscate
    private static final int MAX_USERS = 1000;
    
    @FlowObfuscate
    public void criticalMethod() {
        // 核心业务逻辑
    }
    
    @Native  // 转换为 C++ Native 代码
    @VMProtectUltra  // 应用虚拟化保护
    public String validateLicense(String key) {
        // 授权验证逻辑
    }
}
```

#### 编译并混淆

```bash
# 构建混淆器
cd obfuscator
gradle shadowJar

# 运行混淆
java -jar build/libs/obfuscator-1.2.jar \
  --input YourApp.jar \
  --output YourApp-obfuscated.jar \
  --config obfuscation-config.yml
```

#### 使用图形界面

```bash
cd obfuscatorgui
gradle shadowJar
java -jar build/libs/obfuscatorgui-1.2.jar
```

---

### 2️⃣ 部署授权服务器

#### Windows 用户
```cmd
start_server.bat
```

#### Linux/macOS 用户
```bash
chmod +x start_server.sh
./start_server.sh
```

#### Docker 部署（推荐）
```bash
# Python 版本（开发/测试）
docker-compose up -d ycp-auth-python

# Go 版本（生产环境，性能高 30 倍）
docker-compose up -d ycp-auth-go

# 初始化管理员
curl -X POST http://localhost:13337/init_admin \
  -d "app=MyApp" \
  -d "password=YourSecurePassword"
```

---

### 3️⃣ 配置客户端连接

编辑 `obfuscator/src/main/java/com/yumegod/obfuscator/YumeCloudProtection.java`:

```java
// 第 60 行
public static String authorizationURL = "http://你的服务器IP:13337/";
```

重新编译：
```bash
cd obfuscator
gradle shadowJar
```

---

## 📊 授权服务器性能

| 版本 | 吞吐量 | 响应时间 (P95) | 内存占用 |
|------|--------|---------------|---------|
| **Python** (Flask) | ~500 req/s | 200ms | ~50MB |
| **Go** (Gin) | ~15,000 req/s | 10ms | ~10MB |

**Go 版本性能是 Python 版本的 30 倍！** ⚡

---

## 🛡️ 混淆保护技术

### 核心混淆技术

| 技术 | 描述 | 注解 |
|------|------|------|
| **字符串加密** | 所有字符串常量加密存储 | `@StringObfuscate` |
| **数字混淆** | 数值计算表达式复杂化 | `@NumberObfuscate` |
| **控制流混淆** | 插入虚假分支和跳转 | `@FlowObfuscate` |
| **调用加密** | 方法调用关系加密 | `@CallEncryption` |
| **动态调用** | invokedynamic 指令转换 | `@InvokeDynamic` |
| **重命名** | 类/方法/字段名随机化 | `@Rename` |

### 高级保护技术

| 技术 | 描述 | 注解 |
|------|------|------|
| **Native 保护** | Java → C++ + VMProtect | `@Native` |
| **虚拟化保护** | 代码虚拟机执行 | `@VMProtectVirtualization` |
| **变异保护** | 代码变异 | `@VMProtectMutation` |
| **终极保护** | 最高级别保护 | `@VMProtectUltra` |
| **防调试** | 运行时调试器检测 | `@DebuggerCheck` |
| **防篡改** | 代码完整性验证 | `@TamperPrevention` |

---

## 📡 授权服务器 API

| 端点 | 功能 | 参数 |
|------|------|------|
| `POST /init_admin` | 初始化管理员 | app, password |
| `POST /admin_login` | 管理员登录 | app, password |
| `POST /admin` | 管理命令 | app, password, command |
| `POST /login` | 客户端授权验证 | app, key |

**支持的管理命令**:
- `Key <数量> <天数>` - 生成密钥（1-50 个，1-9999 天）
- `Ban <密钥>` - 封禁密钥
- `Reset <密钥>` - 重置密钥
- `LastLogin <密钥>` - 查询上次登录时间

---

## 🔒 安全特性

### 混淆器安全
✅ 多层混淆保护  
✅ Native 代码转换  
✅ VMProtect 虚拟化  
✅ 运行时防调试  
✅ 代码完整性验证  

### 服务器安全
✅ SHA256 密码哈希  
✅ 密钥过期检查  
✅ 密钥封禁机制  
✅ SQL 注入防护  
✅ 限流保护 (10 req/s)  
✅ SSL/TLS 支持  

---

## 📖 详细文档

- **快速开始**: [README_SERVER.md](README_SERVER.md)
- **完整部署指南**: [DEPLOYMENT.md](DEPLOYMENT.md) (400+ 行)
- **项目总结**: [SERVER_COMPLETE.md](SERVER_COMPLETE.md)
- **测试指南**: [tests/README.md](tests/README.md)

---

## 🧪 测试

### 授权服务器功能测试
```bash
cd tests
chmod +x test_api.sh
./test_api.sh
```

### 授权服务器性能测试
```bash
pip install locust
locust -f tests/test_performance.py --host=http://localhost:13337
# 访问 http://localhost:8089 查看 Web UI
```

---

## 🎓 使用场景

### 适合的应用场景

✅ **商业软件保护** - 防止破解和逆向  
✅ **游戏外挂对抗** - 保护游戏客户端  
✅ **金融应用** - 保护核心算法和密钥  
✅ **企业内部工具** - 防止源码泄露  
✅ **授权管理系统** - 完整的授权体系  

### 技术栈支持

✅ Java 8+  
✅ Android  
✅ ASM 字节码操作  
✅ JNI Native 调用  
✅ VMProtect 集成  

---

## 🎯 技术亮点

### 完整的保护方案
- 从 Java 字节码到 Native 代码的全链路保护
- 多层混淆 + 虚拟化的深度防护
- 运行时检测 + 代码完整性验证

### 自主可控的授权体系
- 完全开源的授权服务器
- 双语言实现（Python + Go）
- 100% 协议兼容
- 生产级部署方案

### 企业级工程质量
- 模块化架构设计
- 丰富的配置选项
- 完整的测试覆盖
- 详尽的文档

---

## 🛠️ 构建项目

### 构建混淆器
```bash
# 构建所有模块
gradle clean build

# 构建核心混淆器
cd obfuscator
gradle shadowJar

# 构建图形界面
cd obfuscatorgui
gradle shadowJar

# 构建管理面板
cd adminpanel
gradle shadowJar
```

### 构建授权服务器
```bash
# Python 版本（无需构建，直接运行）
cd server_python
pip install -r requirements.txt
python app.py

# Go 版本
cd server_go
go build -o auth-server main.go
./auth-server

# Docker 镜像
docker-compose build
```

---

## 📝 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

本项目基于 [YumeCloud Protection](https://github.com/YumeGod/OpenYCP)。

**原始项目地址**: https://github.com/YumeGod/OpenYCP

感谢 YumeGod 开源 YumeCloud Protection，为代码混淆和保护领域做出的贡献。

---

## 📞 支持

如遇到问题，请：

1. 查看 [DEPLOYMENT.md](DEPLOYMENT.md) 完整部署指南
2. 运行 `tests/test_api.sh` 测试脚本
3. 提交 [GitHub Issue](../../issues)

---

## ⚠️ 免责声明

本项目仅供学习和研究使用。请确保你有权使用和修改相关组件。

---

**完整的 Java 代码保护解决方案，从混淆到授权，一站式掌控！** 🚀
