from locust import HttpUser, task, between
import random

class YCPUser(HttpUser):
    """YumeCloud Protection 授权服务器性能测试"""

    wait_time = between(0.1, 0.5)  # 用户请求间隔

    def on_start(self):
        """初始化测试数据"""
        self.app_name = "PerfTestApp"
        self.password = "PerfTestPassword123"

        # 初始化管理员（如果不存在）
        self.client.post("/init_admin", data={
            "app": self.app_name,
            "password": self.password
        })

        # 生成测试密钥
        response = self.client.post("/admin", data={
            "app": self.app_name,
            "password": self.password,
            "command": "Key 100 365"
        })

        if response.status_code == 200:
            self.keys = response.text.strip().split('\n')
        else:
            self.keys = []

    @task(10)
    def client_login(self):
        """客户端授权验证（最常见的操作）"""
        if self.keys:
            key = random.choice(self.keys)
            self.client.post("/login", data={
                "app": self.app_name,
                "key": key
            })

    @task(2)
    def admin_login(self):
        """管理员登录"""
        self.client.post("/admin_login", data={
            "app": self.app_name,
            "password": self.password
        })

    @task(1)
    def check_last_login(self):
        """查询上次登录时间"""
        if self.keys:
            key = random.choice(self.keys)
            self.client.post("/admin", data={
                "app": self.app_name,
                "password": self.password,
                "command": f"LastLogin {key}"
            })

    @task(1)
    def generate_keys(self):
        """生成密钥"""
        self.client.post("/admin", data={
            "app": self.app_name,
            "password": self.password,
            "command": "Key 5 30"
        })

# 运行说明
"""
基础测试（10 个用户，持续 30 秒）:
    locust -f test_performance.py --host=http://localhost:13337 --users 10 --spawn-rate 2 --run-time 30s --headless

压力测试（100 个用户）:
    locust -f test_performance.py --host=http://localhost:13337 --users 100 --spawn-rate 10 --run-time 60s --headless

极限测试（1000 个用户，仅 Go 版本推荐）:
    locust -f test_performance.py --host=http://localhost:13337 --users 1000 --spawn-rate 50 --run-time 120s --headless

Web UI 模式（可视化界面）:
    locust -f test_performance.py --host=http://localhost:13337
    # 然后访问 http://localhost:8089

预期结果:
    Python 版本（Flask + Gunicorn）:
        - 并发用户: 100
        - 吞吐量: ~500 req/s
        - 响应时间: P50=50ms, P95=200ms

    Go 版本（Gin）:
        - 并发用户: 1000
        - 吞吐量: ~15,000 req/s
        - 响应时间: P50=2ms, P95=10ms
"""
