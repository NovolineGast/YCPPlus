# ===== 阶段 1: 构建前端（admin-web） =====
FROM node:20-alpine AS frontend

WORKDIR /web
COPY admin-web/frontend/package.json admin-web/frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY admin-web/frontend/ ./
RUN npm run build

# ===== 阶段 2: 运行镜像 =====
FROM python:3.11-slim

WORKDIR /app

# 安装依赖
COPY server_python/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用与前端构建产物
COPY server_python/app.py .
COPY --from=frontend /web/dist ./web

# 暴露端口（授权协议 + Web 管理面板同端口）
EXPOSE 13337

# 使用 Gunicorn 运行
CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:13337", "--access-logfile", "-", "app:app"]
