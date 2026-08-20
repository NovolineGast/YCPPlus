# YumeCloud Protection 授权服务器 (Python 版)

## 快速部署

### 1. 安装依赖
```bash
pip install -r requirements.txt
```

### 2. 初始化数据库并启动
```bash
python app.py
```

服务器将在 `http://0.0.0.0:13337` 启动

### 3. 创建第一个管理员账号
```bash
curl -X POST http://localhost:13337/init_admin \
  -d "app=MyApp" \
  -d "password=your_secure_password"
```

## 配置客户端

### 修改 YumeCloudProtection 配置
编辑 `obfuscator/src/main/java/com/yumegod/obfuscator/YumeCloudProtection.java`:

```java
// 第 60 行
public static String authorizationURL = "http://your-server-ip:13337/";
```

或在配置文件中设置：
```yaml
native:
  auth: true
auth_url: "http://your-server-ip:13337/"
```

### Admin Panel 使用
启动 Admin Panel：
```bash
cd adminpanel
gradle shadowJar
java -jar build/libs/adminpanel-1.2.jar
```

在 URL 字段填入：`http://your-server-ip:13337/`

## API 端点

### 1. 管理员登录
```http
POST /admin_login
Content-Type: application/x-www-form-urlencoded

app=MyApp&password=your_password
```

返回: `success` 或错误信息

### 2. 管理命令
```http
POST /admin
Content-Type: application/x-www-form-urlencoded

app=MyApp&password=your_password&command=Key 10 30
```

**支持的命令**:
- `Key <数量> <天数>` - 生成密钥 (1-50个, 1-9999天)
- `Ban <密钥>` - 封禁密钥
- `Reset <密钥>` - 重置密钥（清除登录记录）
- `LastLogin <密钥>` - 查询上次登录时间

### 3. 客户端授权验证
```http
POST /login
Content-Type: application/x-www-form-urlencoded

app=MyApp&key=MyApp_xxxxxxxxxxxxxxxxxxxxx
```

返回: `success` 或错误信息

## 生产环境部署

### 使用 Gunicorn (推荐)
```bash
gunicorn -w 4 -b 0.0.0.0:13337 app:app
```

### 使用 Systemd 服务
创建 `/etc/systemd/system/ycp-auth.service`:

```ini
[Unit]
Description=YumeCloud Protection Auth Server
After=network.target

[Service]
Type=notify
User=www-data
WorkingDirectory=/opt/ycp-auth
Environment="PATH=/opt/ycp-auth/venv/bin"
ExecStart=/opt/ycp-auth/venv/bin/gunicorn -w 4 -b 0.0.0.0:13337 app:app
Restart=always

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl enable ycp-auth
sudo systemctl start ycp-auth
```

### 使用 Nginx 反向代理
```nginx
server {
    listen 80;
    server_name auth.yourdomain.com;

    location / {
        proxy_pass http://127.0.0.1:13337;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Docker 部署
创建 `Dockerfile`:
```dockerfile
FROM python:3.11-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 13337

CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:13337", "app:app"]
```

构建并运行：
```bash
docker build -t ycp-auth .
docker run -d -p 13337:13337 -v ./ycp_auth.db:/app/ycp_auth.db ycp-auth
```

## 数据库管理

### 查看所有密钥
```bash
sqlite3 ycp_auth.db "SELECT * FROM keys WHERE app_name='MyApp';"
```

### 手动封禁密钥
```bash
sqlite3 ycp_auth.db "UPDATE keys SET is_banned=1 WHERE key_id='MyApp_xxxxx';"
```

### 导出密钥列表
```bash
sqlite3 ycp_auth.db -header -csv "SELECT key_id, expire_date, is_banned FROM keys;" > keys_backup.csv
```

## 安全建议

1. **启用 HTTPS**: 生产环境必须使用 SSL/TLS
2. **防火墙规则**: 仅开放 13337 端口给受信任的 IP
3. **强密码策略**: 管理员密码至少 16 位
4. **定期备份**: 每天备份 `ycp_auth.db`
5. **日志审计**: 记录所有登录和密钥操作

## 故障排查

### 问题：客户端连接超时
- 检查防火墙：`sudo ufw allow 13337/tcp`
- 检查服务状态：`systemctl status ycp-auth`

### 问题：密钥验证失败
- 检查密钥格式：必须以 `AppName_` 开头
- 检查数据库：`sqlite3 ycp_auth.db "SELECT * FROM keys WHERE key_id='xxx';"`

### 问题：Admin Panel 无法登录
- 验证 URL 末尾有 `/`：`http://ip:13337/`
- 检查应用名和密码是否正确
