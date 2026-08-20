from flask import Flask, request, jsonify, send_from_directory
from functools import wraps
import sqlite3
import hashlib
import hmac
import base64
import json
import math
import os
import secrets
import datetime

DB_PATH = os.environ.get('YCP_DB_PATH', 'ycp_auth.db')
WEB_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'web')
JWT_EXPIRATION = 86400  # 24 小时（秒）

app = Flask(__name__, static_folder=WEB_DIR, static_url_path='')


# ===== 数据库 =====

def get_conn():
    return sqlite3.connect(DB_PATH)


def init_db():
    conn = get_conn()
    c = conn.cursor()

    # 应用表（管理员账号，Web 面板与原生协议共用）
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
                  login_count INTEGER DEFAULT 0,
                  FOREIGN KEY (app_name) REFERENCES applications(app_name))''')

    # 旧库迁移：补充 login_count 列（已存在时报错可忽略）
    try:
        c.execute("ALTER TABLE keys ADD COLUMN login_count INTEGER DEFAULT 0")
    except sqlite3.OperationalError:
        pass

    # 设置表（存 JWT 密钥，保证重启/多 worker 后 token 仍有效）
    c.execute('''CREATE TABLE IF NOT EXISTS settings
                 (name TEXT PRIMARY KEY,
                  value TEXT NOT NULL)''')

    conn.commit()
    conn.close()


def hash_password(password):
    return hashlib.sha256(password.encode()).hexdigest()


def generate_key(app_name, days):
    key = f"{app_name}_{secrets.token_urlsafe(32)}"
    expire_date = (datetime.datetime.now() + datetime.timedelta(days=days)).strftime('%Y-%m-%d %H:%M:%S')
    return key, expire_date


# ===== JWT (HS256，标准库实现) =====

def _b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode()


def _b64url_decode(data: str) -> bytes:
    padding = '=' * (-len(data) % 4)
    return base64.urlsafe_b64decode(data + padding)


def get_jwt_secret() -> str:
    conn = get_conn()
    c = conn.cursor()
    c.execute("SELECT value FROM settings WHERE name='jwt_secret'")
    row = c.fetchone()
    if row is None:
        secret = secrets.token_hex(32)
        c.execute("INSERT INTO settings (name, value) VALUES ('jwt_secret', ?)", (secret,))
        conn.commit()
    else:
        secret = row[0]
    conn.close()
    return secret


def generate_jwt(app_name: str) -> str:
    now = int(datetime.datetime.now().timestamp())
    header = _b64url(json.dumps({"alg": "HS256", "typ": "JWT"}).encode())
    payload = _b64url(json.dumps({
        "sub": app_name,
        "iat": now,
        "exp": now + JWT_EXPIRATION,
    }).encode())
    signing_input = f"{header}.{payload}"
    sig = hmac.new(get_jwt_secret().encode(), signing_input.encode(), hashlib.sha256).digest()
    return f"{signing_input}.{_b64url(sig)}"


def validate_jwt(token: str):
    """校验通过返回 app_name，否则返回 None"""
    parts = token.split('.')
    if len(parts) != 3:
        return None
    signing_input = f"{parts[0]}.{parts[1]}"
    expected = _b64url(hmac.new(get_jwt_secret().encode(), signing_input.encode(), hashlib.sha256).digest())
    if not hmac.compare_digest(expected, parts[2]):
        return None
    try:
        claims = json.loads(_b64url_decode(parts[1]))
    except (ValueError, json.JSONDecodeError):
        return None
    if datetime.datetime.now().timestamp() >= claims.get('exp', 0):
        return None
    return claims.get('sub')


# ===== CORS =====

@app.after_request
def add_cors_headers(response):
    response.headers['Access-Control-Allow-Origin'] = '*'
    response.headers['Access-Control-Allow-Headers'] = 'Authorization, Content-Type'
    response.headers['Access-Control-Allow-Methods'] = 'GET, POST, DELETE, OPTIONS'
    return response


# ===== 原生协议 API（表单） =====

def require_auth(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        app_name = request.form.get('app')
        password = request.form.get('password')

        if not app_name or not password:
            return "Missing credentials", 403

        conn = get_conn()
        c = conn.cursor()
        c.execute("SELECT password_hash FROM applications WHERE app_name=?", (app_name,))
        result = c.fetchone()
        conn.close()

        if not result or result[0] != hash_password(password):
            return "Invalid credentials", 403

        return f(*args, **kwargs)
    return decorated_function


@app.route('/admin_login', methods=['POST'])
def admin_login():
    """管理员登录（表单，供原生 Admin Panel 调用）"""
    app_name = request.form.get('app')
    password = request.form.get('password')

    conn = get_conn()
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
    """管理命令统一入口（表单）"""
    command = request.form.get('command', '')
    app_name = request.form.get('app')

    conn = get_conn()
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
            c.execute("UPDATE keys SET last_login=NULL, login_count=0 WHERE key_id=? AND app_name=?", (key, app_name))
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

    conn = get_conn()
    c = conn.cursor()

    c.execute("""SELECT expire_date, is_banned
                 FROM keys
                 WHERE key_id=? AND app_name=?""", (key, app_name))
    result = c.fetchone()

    if not result:
        conn.close()
        return "Invalid key", 403

    expire_date, is_banned = result

    if is_banned:
        conn.close()
        return "Key banned", 403

    expire_dt = datetime.datetime.strptime(expire_date, '%Y-%m-%d %H:%M:%S')
    if datetime.datetime.now() > expire_dt:
        conn.close()
        return "Key expired", 403

    # 更新最后登录时间和登录次数
    now = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    c.execute("UPDATE keys SET last_login=?, login_count=login_count+1 WHERE key_id=?", (now, key))
    conn.commit()
    conn.close()

    return "success"


@app.route('/init_admin', methods=['POST'])
def init_admin():
    """初始化管理员账号（表单，仅用于首次部署）"""
    app_name = request.form.get('app')
    password = request.form.get('password')

    if not app_name or not password:
        return "Missing parameters"

    conn = get_conn()
    c = conn.cursor()

    c.execute("SELECT 1 FROM applications WHERE app_name=?", (app_name,))
    if c.fetchone():
        conn.close()
        return "App already exists"

    c.execute("INSERT INTO applications (app_name, password_hash, created_at) VALUES (?, ?, ?)",
              (app_name, hash_password(password), datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
    conn.commit()
    conn.close()

    return "Admin created successfully"


# ===== Web 管理面板 REST API (JSON + JWT) =====

def require_jwt(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get('Authorization', '')
        token = auth_header.replace('Bearer ', '')
        app_name = validate_jwt(token)
        if not app_name:
            return jsonify({"error": "Invalid or expired token"}), 401
        request.jwt_app_name = app_name
        return f(*args, **kwargs)
    return decorated_function


def fetch_keys(app_name):
    """查询某应用全部密钥，按创建时间倒序"""
    conn = get_conn()
    c = conn.cursor()
    c.execute("""SELECT key_id, expire_date, is_banned, last_login, created_at, login_count
                 FROM keys WHERE app_name=? ORDER BY created_at DESC""", (app_name,))
    rows = c.fetchall()
    conn.close()
    keys = []
    for row in rows:
        expire_dt = datetime.datetime.strptime(row[1], '%Y-%m-%d %H:%M:%S')
        keys.append({
            'key': row[0],
            'expire_date': row[1],
            'expire_dt': expire_dt,
            'banned': row[2] == 1,
            'last_login': row[3],
            'created_at': row[4],
            'login_count': row[5],
        })
    return keys


def db_time_to_iso(s):
    """'2024-08-20 19:30:15' → '2024-08-20T19:30:15'（前端 new Date() 可解析）"""
    return s.replace(' ', 'T', 1) if s else None


def key_to_response(k):
    now = datetime.datetime.now()
    expired = k['expire_dt'] < now
    days_until_expiry = math.floor((k['expire_dt'] - now).total_seconds() / 86400)

    if k['banned']:
        status = 'banned'
    elif expired:
        status = 'expired'
    else:
        status = 'active'

    return {
        'key': k['key'],
        'status': status,
        'createdAt': db_time_to_iso(k['created_at']),
        'expiresAt': db_time_to_iso(k['expire_date']),
        'lastLogin': db_time_to_iso(k['last_login']),
        'loginCount': k['login_count'],
        'daysUntilExpiry': days_until_expiry,
    }


@app.route('/api/init', methods=['POST'])
def api_init():
    """Web 面板：初始化管理员"""
    data = request.get_json(silent=True) or {}
    app_name = data.get('appName', '')
    password = data.get('password', '')

    if not app_name or not password:
        return jsonify({"error": "Missing parameters"}), 400

    conn = get_conn()
    c = conn.cursor()
    c.execute("SELECT 1 FROM applications WHERE app_name=?", (app_name,))
    if c.fetchone():
        conn.close()
        return jsonify({"error": f"Admin already exists for app: {app_name}"}), 400

    c.execute("INSERT INTO applications (app_name, password_hash, created_at) VALUES (?, ?, ?)",
              (app_name, hash_password(password), datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
    conn.commit()
    conn.close()

    return jsonify({"message": "Admin initialized successfully"})


@app.route('/api/login', methods=['POST'])
def api_login():
    """Web 面板：登录，返回 JWT"""
    data = request.get_json(silent=True) or {}
    app_name = data.get('appName', '')
    password = data.get('password', '')

    conn = get_conn()
    c = conn.cursor()
    c.execute("SELECT password_hash FROM applications WHERE app_name=?", (app_name,))
    result = c.fetchone()
    conn.close()

    if not result or result[0] != hash_password(password):
        return jsonify({"error": "Invalid credentials"}), 400

    return jsonify({
        "token": generate_jwt(app_name),
        "appName": app_name,
        "expiresIn": JWT_EXPIRATION,
    })


@app.route('/api/dashboard/stats', methods=['GET'])
@require_jwt
def api_stats():
    """Web 面板：仪表盘统计"""
    app_name = request.jwt_app_name
    keys = fetch_keys(app_name)
    now = datetime.datetime.now()

    stats = {
        'totalKeys': len(keys),
        'activeKeys': sum(1 for k in keys if not k['banned'] and k['expire_dt'] > now),
        'expiredKeys': sum(1 for k in keys if k['expire_dt'] <= now),
        'bannedKeys': sum(1 for k in keys if k['banned']),
        'expiringSoon': sum(1 for k in keys
                            if 0 <= math.floor((k['expire_dt'] - now).total_seconds() / 86400) <= 7),
        'totalLogins': sum(k['login_count'] for k in keys),
    }
    return jsonify(stats)


@app.route('/api/keys', methods=['GET'])
@require_jwt
def api_list_keys():
    """Web 面板：密钥列表"""
    keys = fetch_keys(request.jwt_app_name)
    return jsonify([key_to_response(k) for k in keys])


KEY_CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"  # 排除易混淆字符


@app.route('/api/keys/generate', methods=['POST'])
@require_jwt
def api_generate_keys():
    """Web 面板：生成密钥（格式 PREFIX-XXXX-XXXX-XXXX）"""
    data = request.get_json(silent=True) or {}
    amount = data.get('amount', 0)
    days = data.get('days', 0)
    prefix = (data.get('prefix') or '').strip().upper()

    if not isinstance(amount, int) or not (1 <= amount <= 50):
        return jsonify({"error": "Amount must be between 1 and 50"}), 400
    if not isinstance(days, int) or not (1 <= days <= 9999):
        return jsonify({"error": "Days must be between 1 and 9999"}), 400

    prefix = prefix if prefix else "YCP"

    now = datetime.datetime.now()
    expire_date = (now + datetime.timedelta(days=days)).strftime('%Y-%m-%d %H:%M:%S')

    conn = get_conn()
    c = conn.cursor()
    keys = []
    try:
        for _ in range(amount):
            parts = [''.join(secrets.choice(KEY_CHARSET) for _ in range(4)) for _ in range(3)]
            key = f"{prefix}-{'-'.join(parts)}"
            c.execute("INSERT INTO keys (key_id, app_name, expire_date, created_at) VALUES (?, ?, ?, ?)",
                      (key, request.jwt_app_name, expire_date, now.strftime('%Y-%m-%d %H:%M:%S')))
            keys.append(key)
        conn.commit()
    except Exception:
        conn.close()
        return jsonify({"error": "Database error"}), 500
    conn.close()

    return jsonify({"keys": keys})


@app.route('/api/keys/<key>/ban', methods=['POST'])
@require_jwt
def api_ban_key(key):
    """Web 面板：封禁密钥"""
    conn = get_conn()
    c = conn.cursor()
    try:
        c.execute("UPDATE keys SET is_banned=1 WHERE key_id=? AND app_name=?", (key, request.jwt_app_name))
        conn.commit()
    finally:
        conn.close()
    return jsonify({"message": "Key banned"})


@app.route('/api/keys/<key>/unban', methods=['POST'])
@require_jwt
def api_unban_key(key):
    """Web 面板：解封密钥"""
    conn = get_conn()
    c = conn.cursor()
    try:
        c.execute("UPDATE keys SET is_banned=0 WHERE key_id=? AND app_name=?", (key, request.jwt_app_name))
        conn.commit()
    finally:
        conn.close()
    return jsonify({"message": "Key unbanned"})


@app.route('/api/keys/<key>', methods=['DELETE'])
@require_jwt
def api_delete_key(key):
    """Web 面板：删除密钥"""
    conn = get_conn()
    c = conn.cursor()
    try:
        c.execute("DELETE FROM keys WHERE key_id=? AND app_name=?", (key, request.jwt_app_name))
        conn.commit()
    finally:
        conn.close()
    return jsonify({"message": "Key deleted"})


@app.route('/api/keys/<key>/fingerprint', methods=['GET'])
def api_fingerprint(key):
    """Web 面板：密钥指纹（SHA256 前 4 字节十六进制）"""
    fingerprint = hashlib.sha256(key.encode()).hexdigest()[:8].upper()
    return jsonify({"fingerprint": fingerprint})


# ===== 前端静态文件（SPA） =====

@app.route('/')
def web_index():
    return send_from_directory(WEB_DIR, 'index.html')


@app.errorhandler(404)
def web_fallback(e):
    """非 /api 路径回退到 SPA index.html"""
    if not request.path.startswith('/api'):
        try:
            return send_from_directory(WEB_DIR, 'index.html')
        except FileNotFoundError:
            return "Admin web panel is not built yet. Run: cd admin-web/frontend && npm install && npm run build, then copy dist/ into server_python/web/.", 404
    return jsonify({"error": "Not found"}), 404


# 模块导入时初始化数据库（gunicorn 多 worker 下 __main__ 不会执行）
init_db()

if __name__ == '__main__':
    # 生产环境使用 gunicorn: gunicorn -w 4 -b 0.0.0.0:13337 app:app
    app.run(host='0.0.0.0', port=13337, debug=False)
