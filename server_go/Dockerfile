# ===== 阶段 1: 构建前端（admin-web） =====
FROM node:20-alpine AS frontend

WORKDIR /web
COPY admin-web/frontend/package.json admin-web/frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY admin-web/frontend/ ./
RUN npm run build

# ===== 阶段 2: 编译 Go 服务器（嵌入前端） =====
FROM golang:1.21-alpine AS builder

# 安装编译依赖
RUN apk add --no-cache gcc musl-dev sqlite-dev

WORKDIR /build

# 复制依赖文件
COPY server_go/go.mod server_go/go.sum ./
RUN go mod download

# 复制源码与前端构建产物（embed 需要 web/ 目录）
COPY server_go/main.go ./
COPY --from=frontend /web/dist ./web

RUN CGO_ENABLED=1 GOOS=linux go build -a -ldflags '-extldflags "-static"' -o ycp-auth .

# ===== 阶段 3: 运行镜像 =====
FROM alpine:latest

# 安装运行时依赖
RUN apk --no-cache add ca-certificates sqlite-libs

WORKDIR /app

# 从构建阶段复制二进制文件（前端已嵌入）
COPY --from=builder /build/ycp-auth .

# 创建数据目录
RUN mkdir -p /app/data

# 暴露端口（授权协议 + Web 管理面板同端口）
EXPOSE 13337

# 运行应用
CMD ["./ycp-auth"]
