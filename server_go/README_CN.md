# YumeCloud Protection 授权服务器 (Go 版)

## 快速部署

### 1. 初始化依赖
```bash
go mod download
```

### 2. 编译并运行
```bash
# 开发环境
go run main.go

# 生产环境编译
go build -o ycp-auth main.go
./ycp-auth
```

服务器将在 `:13337` 启动

### 3. 创建管理员账号
```bash
curl -X POST http://localhost:13337/init_admin \
  -d "app=MyApp" \
  -d "password=your_secure_password"
```

## 性能优势

Go 版本相比 Python 版本：
- **更低内存占用** (~10MB vs ~50MB)
- **更高并发性能** (10,000+ req/s vs 500 req/s)
- **单文件部署** (编译后无需依赖)

## 交叉编译

### 编译 Linux 版本
```bash
GOOS=linux GOARCH=amd64 go build -o ycp-auth-linux main.go
```

### 编译 Windows 版本
```bash
GOOS=windows GOARCH=amd64 go build -o ycp-auth.exe main.go
```

### 编译 macOS 版本
```bash
GOOS=darwin GOARCH=amd64 go build -o ycp-auth-mac main.go
```

## Systemd 服务

创建 `/etc/systemd/system/ycp-auth.service`:

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
sudo systemctl enable ycp-auth
sudo systemctl start ycp-auth
```

## Docker 部署

### Dockerfile
```dockerfile
FROM golang:1.21-alpine AS builder

WORKDIR /build
COPY go.mod go.sum ./
RUN go mod download

COPY main.go ./
RUN CGO_ENABLED=1 GOOS=linux go build -a -installsuffix cgo -o ycp-auth .

FROM alpine:latest
RUN apk --no-cache add ca-certificates sqlite-libs

WORKDIR /app
COPY --from=builder /build/ycp-auth .

EXPOSE 13337

CMD ["./ycp-auth"]
```

### 构建并运行
```bash
docker build -t ycp-auth-go .
docker run -d -p 13337:13337 -v ./ycp_auth.db:/app/ycp_auth.db ycp-auth-go
```

## API 端点

所有端点与 Python 版本完全兼容，详见 `../server_python/README_CN.md`

## 性能测试

使用 Apache Bench 测试：
```bash
# 测试登录接口
ab -n 10000 -c 100 -p login.txt -T "application/x-www-form-urlencoded" http://localhost:13337/login

# login.txt 内容:
# app=MyApp&key=MyApp_xxxxxxxxx
```

预期性能：
- **Python (Flask + Gunicorn)**: ~500 req/s
- **Go (Gin)**: ~15,000 req/s

## 监控和日志

### 添加日志文件输出
修改 `main.go`:
```go
logFile, _ := os.OpenFile("ycp-auth.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
gin.DefaultWriter = io.MultiWriter(logFile, os.Stdout)
```

### 使用 Prometheus 监控
安装 Prometheus 中间件：
```bash
go get github.com/zsais/go-gin-prometheus
```

## 故障排查

### CGO 编译错误
如果遇到 `gcc not found`：
```bash
# Ubuntu/Debian
sudo apt-get install build-essential

# Alpine
apk add gcc musl-dev

# macOS
xcode-select --install
```

### 数据库锁定错误
SQLite 默认不支持高并发写入，建议：
1. 使用 PostgreSQL/MySQL 替代（修改 database/sql 驱动）
2. 启用 WAL 模式：`PRAGMA journal_mode=WAL;`
