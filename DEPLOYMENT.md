# YumeCloud Protection 自建授权服务器完整指南

## 📦 项目结构

```
OpenYCP/
├── server_python/          # Python Flask 实现（含 Web 管理面板）
│   ├── app.py             # 主应用（授权 API + Web 面板 API + 静态文件）
│   ├── requirements.txt   # Python 依赖
│   ├── Dockerfile         # Docker 镜像（多阶段：自动构建前端）
│   ├── web/               # 前端构建产物（npm run build 生成）
│   └── README_CN.md       # Python 版文档
├── server_go/             # Go Gin 实现（高性能，含 Web 管理面板）
│   ├── main.go            # 主应用（授权 API + Web 面板 API + embed 前端）
│   ├── go.mod             # Go 依赖
│   ├── Dockerfile         # Docker 镜像（多阶段：自动构建前端）
│   ├── web/               # 前端构建产物（embed 嵌入二进制）
│   └── README_CN.md       # Go 版文档
├── admin-web/             # Web 管理面板前端源码（React，构建后嵌入服务器）
├── nginx/                 # Nginx 反向代理配置
│   └── nginx.conf
├── docker-compose.yml     # Docker Compose 编排
└── DEPLOYMENT.md          # 本文档
```

> **Web 管理面板已内置**: 启动任一授权服务器后，浏览器访问 `http://localhost:13337/` 即可使用 Web 管理面板（登录页点 "First time? Initialize admin" 初始化账号，与原生协议共用同一账号体系）。

---

## 🚀 快速开始（3 种方式）

### 方式 1: 本地运行 Python 版（推荐新手）

```bash
cd server_python
pip install -r requirements.txt
python app.py
```

**初始化管理员**：
```bash
curl -X POST http://localhost:13337/init_admin \
  -d "app=MyApp" \
  -d "password=YourSecurePassword123"
```

**测试登录**：
```bash
curl -X POST http://localhost:13337/admin_login \
  -d "app=MyApp" \
  -d "password=YourSecurePassword123"
```

### 方式 2: 本地运行 Go 版（推荐生产）

```bash
cd server_go
go mod download
go run main.go
```

初始化管理员的方式相同。

### 方式 3: Docker 一键部署（最简单）

```bash
# 启动 Python 版本
docker-compose up -d ycp-auth-python

# 或启动 Go 版本（更高性能）
docker-compose up -d ycp-auth-go

# 初始化管理员
docker exec ycp-auth-python curl -X POST http://localhost:13337/init_admin \
  -d "app=MyApp" \
  -d "password=YourSecurePassword123"
```

---

## 🔧 配置 YumeCloud Protection 客户端

### 1. 修改授权服务器地址

编辑 `obfuscator/src/main/java/com/yumegod/obfuscator/YumeCloudProtection.java`:

```java
// 第 60 行
public static String authorizationURL = "http://your-server-ip:13337/";
```

或者在配置文件中设置：

```yaml
native:
  auth: true
# auth_url: "http://your-server-ip:13337/"
```

### 2. 重新编译 Obfuscator

```bash
cd obfuscator
gradle shadowJar
```

### 3. 使用 Admin Panel 管理密钥

启动 Admin Panel：
```bash
cd adminpanel
gradle shadowJar
java -jar build/libs/adminpanel-1.2.jar
```

在 GUI 中：
- **App name**: MyApp
- **Password**: YourSecurePassword123
- **URL**: http://your-server-ip:13337/

点击 **Login** 后即可管理密钥。

---

## 📊 API 完整文档

### 1. `/init_admin` - 初始化管理员（仅首次）

```http
POST /init_admin HTTP/1.1
Content-Type: application/x-www-form-urlencoded

app=MyApp&password=SecurePassword123
```

**响应**：
- `Admin created successfully` - 成功
- `App already exists` - 应用已存在
- `Missing parameters` - 缺少参数

### 2. `/admin_login` - 管理员登录

```http
POST /admin_login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

app=MyApp&password=SecurePassword123
```

**响应**：
- `success` - 登录成功
- `Invalid app name or password` - 凭据错误

### 3. `/admin` - 管理命令

**需要认证**（通过 `app` 和 `password` 参数）

#### 3.1 生成密钥
```http
POST /admin HTTP/1.1
Content-Type: application/x-www-form-urlencoded

app=MyApp&password=SecurePassword123&command=Key 10 30
```

参数说明：
- `Key <数量> <天数>` 
- 数量: 1-50
- 天数: 1-9999

**响应**：
```
MyApp_abc123def456...
MyApp_ghi789jkl012...
...（换行分隔的密钥列表）
```

#### 3.2 封禁密钥
```http
POST /admin HTTP/1.1
Content-Type: application/x-www-form-urlencoded

app=MyApp&password=SecurePassword123&command=Ban MyApp_abc123def456
```

**响应**：
- `success` - 封禁成功
- `Key not found` - 密钥不存在

#### 3.3 重置密钥（清除登录记录）
```http
POST /admin HTTP/1.1
Content-Type: application/x-www-form-urlencoded

app=MyApp&password=SecurePassword123&command=Reset MyApp_abc123def456
```

**响应**：同上

#### 3.4 查询上次登录时间
```http
POST /admin HTTP/1.1
Content-Type: application/x-www-form-urlencoded

app=MyApp&password=SecurePassword123&command=LastLogin MyApp_abc123def456
```

**响应**：
- `2024-08-20 19:30:15` - 时间戳
- `null` - 从未登录
- `Key not found` - 密钥不存在

### 4. `/login` - 客户端授权验证

**由 Native 层调用，用户无需手动调用**

```http
POST /login HTTP/1.1
Content-Type: application/x-www-form-urlencoded

app=MyApp&key=MyApp_abc123def456...
```

**响应**：
- `success` - 验证通过
- `Invalid key` - 密钥不存在
- `Key banned` - 密钥已封禁
- `Key expired` - 密钥已过期

---

## 🔐 生产环境部署

### 1. 使用 Nginx + SSL

#### 获取 SSL 证书（Let's Encrypt）
```bash
sudo apt install certbot
sudo certbot certonly --standalone -d auth.yourdomain.com
```

#### 修改 `nginx/nginx.conf`
取消注释 HTTPS server 块，并修改域名：
```nginx
server {
    listen 443 ssl http2;
    server_name auth.yourdomain.com;
    
    ssl_certificate /etc/letsencrypt/live/auth.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/auth.yourdomain.com/privkey.pem;
    # ...
}
```

#### 启动 Nginx 容器
```bash
docker-compose up -d nginx
```

### 2. 使用 Systemd 服务（推荐 Go 版）

编译 Go 二进制：
```bash
cd server_go
GOOS=linux GOARCH=amd64 go build -o ycp-auth main.go
```

部署到服务器：
```bash
sudo mkdir -p /opt/ycp-auth
sudo cp ycp-auth /opt/ycp-auth/
sudo chown -R www-data:www-data /opt/ycp-auth
```

创建服务文件 `/etc/systemd/system/ycp-auth.service`：
```ini
[Unit]
Description=YumeCloud Protection Auth Server
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/ycp-auth
ExecStart=/opt/ycp-auth/ycp-auth
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable ycp-auth
sudo systemctl start ycp-auth
sudo systemctl status ycp-auth
```

### 3. 防火墙配置

```bash
# UFW (Ubuntu)
sudo ufw allow 13337/tcp

# Firewalld (CentOS/RHEL)
sudo firewall-cmd --permanent --add-port=13337/tcp
sudo firewall-cmd --reload

# iptables
sudo iptables -A INPUT -p tcp --dport 13337 -j ACCEPT
```

### 4. 数据库备份脚本

创建 `/opt/ycp-auth/backup.sh`:
```bash
#!/bin/bash
BACKUP_DIR="/opt/ycp-auth/backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR
cp /opt/ycp-auth/ycp_auth.db $BACKUP_DIR/ycp_auth_$DATE.db

# 保留最近 30 天的备份
find $BACKUP_DIR -name "ycp_auth_*.db" -mtime +30 -delete

echo "Backup completed: ycp_auth_$DATE.db"
```

添加到 crontab（每天 3:00 备份）：
```bash
sudo crontab -e
0 3 * * * /opt/ycp-auth/backup.sh >> /var/log/ycp-backup.log 2>&1
```

---

## 📈 性能测试

### Python 版本（Flask + Gunicorn）

```bash
# 安装测试工具
pip install locust

# 创建 locustfile.py
cat > locustfile.py << 'EOF'
from locust import HttpUser, task

class YCPUser(HttpUser):
    @task
    def login(self):
        self.client.post("/login", data={
            "app": "MyApp",
            "key": "MyApp_test_key_123"
        })
EOF

# 运行测试
locust -f locustfile.py --host=http://localhost:13337
```

**预期性能**：
- 并发: 100 用户
- 吞吐: ~500 req/s
- 响应时间: ~200ms (P95)

### Go 版本（Gin）

使用相同的测试脚本：

**预期性能**：
- 并发: 1000 用户
- 吞吐: ~15,000 req/s
- 响应时间: ~10ms (P95)

---

## 🛠️ 故障排查

### 问题 1: 连接超时

**症状**: Admin Panel 显示 "Exception caught while logging-in"

**解决方案**：
```bash
# 检查服务状态
systemctl status ycp-auth

# 检查端口监听
netstat -tlnp | grep 13337

# 检查防火墙
sudo ufw status
```

### 问题 2: 密钥验证失败

**症状**: Native 层返回 "Invalid key"

**排查步骤**：
```bash
# 1. 检查密钥是否存在
sqlite3 ycp_auth.db "SELECT * FROM keys WHERE key_id='MyApp_xxx';"

# 2. 检查密钥状态
sqlite3 ycp_auth.db "SELECT key_id, expire_date, is_banned FROM keys WHERE key_id='MyApp_xxx';"

# 3. 手动测试授权接口
curl -X POST http://localhost:13337/login \
  -d "app=MyApp" \
  -d "key=MyApp_xxx"
```

### 问题 3: 数据库锁定

**症状**: "database is locked" 错误

**解决方案**：
```bash
# 启用 WAL 模式（提高并发性能）
sqlite3 ycp_auth.db "PRAGMA journal_mode=WAL;"

# 或迁移到 PostgreSQL/MySQL
```

---

## 🔒 安全最佳实践

1. **使用 HTTPS**: 生产环境必须启用 SSL/TLS
2. **强密码策略**: 管理员密码至少 16 位，包含大小写字母、数字、符号
3. **IP 白名单**: 限制授权服务器仅允许受信任的 IP 访问
4. **限流保护**: Nginx 配置中已包含限流规则（10 req/s）
5. **定期备份**: 每天自动备份数据库
6. **日志审计**: 记录所有关键操作（登录、密钥生成、封禁）
7. **隔离部署**: 授权服务器与应用服务器分离部署

---

## 📞 技术支持

如遇到问题，请提供：
1. 服务器版本（Python/Go）
2. 错误日志（`journalctl -u ycp-auth -n 100`）
3. 数据库状态（`SELECT COUNT(*) FROM keys;`）
4. 网络拓扑（客户端 → 服务器连接方式）

---

## 📝 更新日志

- **2024-08-20**: 初始版本发布
  - Python Flask 实现
  - Go Gin 实现
  - Docker Compose 编排
  - Nginx 反向代理配置
