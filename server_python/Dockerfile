FROM python:3.11-slim

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 复制应用
COPY app.py .

# 创建数据目录
RUN mkdir -p /app/data

# 暴露端口
EXPOSE 13337

# 使用 Gunicorn 运行
CMD ["gunicorn", "-w", "4", "-b", "0.0.0.0:13337", "--access-logfile", "-", "app:app"]
