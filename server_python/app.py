from flask import Flask, request, jsonify
import sqlite3
import hashlib
import secrets
import datetime
from functools import wraps

app = Flask(__name__)

# 数据库初始化
def init_db():
    conn = sqlite3.connect('ycp_auth.db')
    c = conn.cursor()

    # 应用表（管理员账号）
    c.execute('''CREATE TABLE IF NOT EXISTS applications
                 (app_name TEXT PRIMARY KEY,
                  password_hash TEXT NOT NULL,
                  created_at TEXT NOT NULL)''')

    # 密钥表
    c.execute('''CREATE TABLE IF NOT EXISTS keys
                 (key_id TEXT PRIMARY KEY,
                  app_name TEXT NOT NULL,
                  expire_date TEXT NOT NULL,
                  is_banned INTEGER DEFAULT 0,
                  last_login TEXT,
                  created_at TEXT NOT NULL,
                  FOREIGN KEY (app_name) REFERENCES applications(app_name))''')

    conn.commit()
    conn.close()

# 密码哈希
def hash_password(password):
    return hashlib.sha256(password.encode()).hexdigest()

# 生成密钥
def generate_key(app_name, days):
    key = f"{app_name}_{secrets.token_urlsafe(32)}"
    expire_date = (datetime.datetime.now() + datetime.timedelta(days=days)).strftime('%Y-%m-%d %H:%M:%S')
    return key, expire_date

# 认证装饰器
def require_auth(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        app_name = request.form.get('app')
        password = request.form.get('password')

        if not app_name or not password:
            return "Missing credentials", 403

        conn = sqlite3.connect('ycp_auth.db')
        c = conn.cursor()
        c.execute("SELECT password_hash FROM applications WHERE app_name=?", (app_name,))
        result = c.fetchone()
        conn.close()

        if not result or result[0] != hash_password(password):
            return "Invalid credentials", 403

        return f(*args, **kwargs)
    return decorated_function

# ===== API 端点 =====

@app.route('/admin_login', methods=['POST'])
def admin_login():
    """管理员登录"""
    app_name = request.form.get('app')
    password = request.form.get('password')

    conn = sqlite3.connect('ycp_auth.db')
    c = conn.cursor()
    c.execute("SELECT password_hash FROM applications WHERE app_name=?", (app_name,))
    result = c.fetchone()
    conn.close()

    if result and result[0] == hash_password(password):
        return "success"
    return "Invalid app name or password"

@app.route('/admin', methods=['POST'])
@require_auth
def admin_command():
    """管理命令统一入口"""
    command = request.form.get('command', '')
    app_name = request.form.get('app')

    conn = sqlite3.connect('ycp_auth.db')
    c = conn.cursor()

    try:
        parts = command.split()
        action = parts[0]

        if action == 'Key':
            # 生成密钥: Key <amount> <days>
            amount = int(parts[1])
            days = int(parts[2])

            if not (1 <= amount <= 50) or not (1 <= days <= 9999):
                return "Invalid parameters"

            keys = []
            for _ in range(amount):
                key, expire_date = generate_key(app_name, days)
                c.execute("INSERT INTO keys (key_id, app_name, expire_date, created_at) VALUES (?, ?, ?, ?)",
                         (key, app_name, expire_date, datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
                keys.append(key)

            conn.commit()
            return "\n".join(keys)

        elif action == 'Ban':
            # 封禁密钥: Ban <key>
            key = parts[1]
            c.execute("UPDATE keys SET is_banned=1 WHERE key_id=? AND app_name=?", (key, app_name))
            conn.commit()
            return "success" if c.rowcount > 0 else "Key not found"

        elif action == 'Reset':
            # 重置密钥: Reset <key>
            key = parts[1]
            c.execute("UPDATE keys SET last_login=NULL WHERE key_id=? AND app_name=?", (key, app_name))
            conn.commit()
            return "success" if c.rowcount > 0 else "Key not found"

        elif action == 'LastLogin':
            # 查询上次登录: LastLogin <key>
            key = parts[1]
            c.execute("SELECT last_login FROM keys WHERE key_id=? AND app_name=?", (key, app_name))
            result = c.fetchone()
            if result:
                return result[0] if result[0] else "null"
            return "Key not found"

        else:
            return "Unknown command"

    except Exception as e:
        return f"Error: {str(e)}"
    finally:
        conn.close()

@app.route('/login', methods=['POST'])
def client_login():
    """客户端授权验证 (C++ Native 层调用)"""
    app_name = request.form.get('app')
    key = request.form.get('key')

    if not app_name or not key:
        return "Missing parameters", 400

    conn = sqlite3.connect('ycp_auth.db')
    c = conn.cursor()

    # 验证密钥
    c.execute("""SELECT expire_date, is_banned
                 FROM keys
                 WHERE key_id=? AND app_name=?""", (key, app_name))
    result = c.fetchone()

    if not result:
        conn.close()
        return "Invalid key", 403

    expire_date, is_banned = result

    # 检查封禁状态
    if is_banned:
        conn.close()
        return "Key banned", 403

    # 检查过期
    expire_dt = datetime.datetime.strptime(expire_date, '%Y-%m-%d %H:%M:%S')
    if datetime.datetime.now() > expire_dt:
        conn.close()
        return "Key expired", 403

    # 更新最后登录时间
    now = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    c.execute("UPDATE keys SET last_login=? WHERE key_id=?", (now, key))
    conn.commit()
    conn.close()

    return "success"

# 初始化脚本
@app.route('/init_admin', methods=['POST'])
def init_admin():
    """初始化管理员账号 (仅用于首次部署)"""
    app_name = request.form.get('app')
    password = request.form.get('password')

    if not app_name or not password:
        return "Missing parameters"

    conn = sqlite3.connect('ycp_auth.db')
    c = conn.cursor()

    # 检查是否已存在
    c.execute("SELECT 1 FROM applications WHERE app_name=?", (app_name,))
    if c.fetchone():
        conn.close()
        return "App already exists"

    # 插入管理员账号
    c.execute("INSERT INTO applications (app_name, password_hash, created_at) VALUES (?, ?, ?)",
             (app_name, hash_password(password), datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
    conn.commit()
    conn.close()

    return "Admin created successfully"

if __name__ == '__main__':
    init_db()
    # 生产环境使用 gunicorn: gunicorn -w 4 -b 0.0.0.0:13337 app:app
    app.run(host='0.0.0.0', port=13337, debug=False)
