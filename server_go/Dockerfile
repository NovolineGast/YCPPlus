FROM golang:1.21-alpine AS builder

# 安装编译依赖
RUN apk add --no-cache gcc musl-dev sqlite-dev

WORKDIR /build

# 复制依赖文件
COPY go.mod go.sum ./
RUN go mod download

# 复制源码并编译
COPY main.go ./
RUN CGO_ENABLED=1 GOOS=linux go build -a -ldflags '-extldflags "-static"' -o ycp-auth .

# 最终镜像
FROM alpine:latest

# 安装运行时依赖
RUN apk --no-cache add ca-certificates sqlite-libs

WORKDIR /app

# 从构建阶段复制二进制文件
COPY --from=builder /build/ycp-auth .

# 创建数据目录
RUN mkdir -p /app/data

# 暴露端口
EXPOSE 13337

# 运行应用
CMD ["./ycp-auth"]
