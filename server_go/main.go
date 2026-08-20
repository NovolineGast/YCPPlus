package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	_ "github.com/mattn/go-sqlite3"
	"github.com/google/uuid"
)

var db *sql.DB

// 初始化数据库
func initDB() error {
	var err error
	db, err = sql.Open("sqlite3", "./ycp_auth.db")
	if err != nil {
		return err
	}

	// 应用表
	_, err = db.Exec(`CREATE TABLE IF NOT EXISTS applications (
		app_name TEXT PRIMARY KEY,
		password_hash TEXT NOT NULL,
		created_at TEXT NOT NULL
	)`)
	if err != nil {
		return err
	}

	// 密钥表
	_, err = db.Exec(`CREATE TABLE IF NOT EXISTS keys (
		key_id TEXT PRIMARY KEY,
		app_name TEXT NOT NULL,
		expire_date TEXT NOT NULL,
		is_banned INTEGER DEFAULT 0,
		last_login TEXT,
		created_at TEXT NOT NULL,
		FOREIGN KEY (app_name) REFERENCES applications(app_name)
	)`)
	return err
}

// 密码哈希
func hashPassword(password string) string {
	hash := sha256.Sum256([]byte(password))
	return hex.EncodeToString(hash[:])
}

// 生成密钥
func generateKey(appName string, days int) (string, string) {
	keyID := fmt.Sprintf("%s_%s", appName, uuid.New().String())
	expireDate := time.Now().AddDate(0, 0, days).Format("2006-01-02 15:04:05")
	return keyID, expireDate
}

// 认证中间件
func requireAuth() gin.HandlerFunc {
	return func(c *gin.Context) {
		appName := c.PostForm("app")
		password := c.PostForm("password")

		if appName == "" || password == "" {
			c.String(http.StatusForbidden, "Missing credentials")
			c.Abort()
			return
		}

		var passwordHash string
		err := db.QueryRow("SELECT password_hash FROM applications WHERE app_name=?", appName).Scan(&passwordHash)
		if err != nil || passwordHash != hashPassword(password) {
			c.String(http.StatusForbidden, "Invalid credentials")
			c.Abort()
			return
		}

		c.Set("app_name", appName)
		c.Next()
	}
}

// 管理员登录
func adminLogin(c *gin.Context) {
	appName := c.PostForm("app")
	password := c.PostForm("password")

	var passwordHash string
	err := db.QueryRow("SELECT password_hash FROM applications WHERE app_name=?", appName).Scan(&passwordHash)

	if err == nil && passwordHash == hashPassword(password) {
		c.String(http.StatusOK, "success")
	} else {
		c.String(http.StatusOK, "Invalid app name or password")
	}
}

// 管理命令
func adminCommand(c *gin.Context) {
	command := c.PostForm("command")
	appName := c.GetString("app_name")

	parts := strings.Split(command, " ")
	if len(parts) == 0 {
		c.String(http.StatusBadRequest, "Empty command")
		return
	}

	action := parts[0]

	switch action {
	case "Key":
		// 生成密钥: Key <amount> <days>
		if len(parts) != 3 {
			c.String(http.StatusBadRequest, "Invalid parameters")
			return
		}

		amount, err1 := strconv.Atoi(parts[1])
		days, err2 := strconv.Atoi(parts[2])

		if err1 != nil || err2 != nil || amount < 1 || amount > 50 || days < 1 || days > 9999 {
			c.String(http.StatusBadRequest, "Invalid parameters")
			return
		}

		var keys []string
		for i := 0; i < amount; i++ {
			keyID, expireDate := generateKey(appName, days)
			_, err := db.Exec("INSERT INTO keys (key_id, app_name, expire_date, created_at) VALUES (?, ?, ?, ?)",
				keyID, appName, expireDate, time.Now().Format("2006-01-02 15:04:05"))
			if err != nil {
				c.String(http.StatusInternalServerError, "Database error")
				return
			}
			keys = append(keys, keyID)
		}

		c.String(http.StatusOK, strings.Join(keys, "\n"))

	case "Ban":
		// 封禁密钥: Ban <key>
		if len(parts) != 2 {
			c.String(http.StatusBadRequest, "Invalid parameters")
			return
		}

		result, err := db.Exec("UPDATE keys SET is_banned=1 WHERE key_id=? AND app_name=?", parts[1], appName)
		if err != nil {
			c.String(http.StatusInternalServerError, "Database error")
			return
		}

		affected, _ := result.RowsAffected()
		if affected > 0 {
			c.String(http.StatusOK, "success")
		} else {
			c.String(http.StatusOK, "Key not found")
		}

	case "Reset":
		// 重置密钥: Reset <key>
		if len(parts) != 2 {
			c.String(http.StatusBadRequest, "Invalid parameters")
			return
		}

		result, err := db.Exec("UPDATE keys SET last_login=NULL WHERE key_id=? AND app_name=?", parts[1], appName)
		if err != nil {
			c.String(http.StatusInternalServerError, "Database error")
			return
		}

		affected, _ := result.RowsAffected()
		if affected > 0 {
			c.String(http.StatusOK, "success")
		} else {
			c.String(http.StatusOK, "Key not found")
		}

	case "LastLogin":
		// 查询上次登录: LastLogin <key>
		if len(parts) != 2 {
			c.String(http.StatusBadRequest, "Invalid parameters")
			return
		}

		var lastLogin sql.NullString
		err := db.QueryRow("SELECT last_login FROM keys WHERE key_id=? AND app_name=?", parts[1], appName).Scan(&lastLogin)

		if err != nil {
			c.String(http.StatusOK, "Key not found")
		} else if lastLogin.Valid {
			c.String(http.StatusOK, lastLogin.String)
		} else {
			c.String(http.StatusOK, "null")
		}

	default:
		c.String(http.StatusBadRequest, "Unknown command")
	}
}

// 客户端登录验证
func clientLogin(c *gin.Context) {
	appName := c.PostForm("app")
	key := c.PostForm("key")

	if appName == "" || key == "" {
		c.String(http.StatusBadRequest, "Missing parameters")
		return
	}

	var expireDate string
	var isBanned int
	err := db.QueryRow("SELECT expire_date, is_banned FROM keys WHERE key_id=? AND app_name=?", key, appName).Scan(&expireDate, &isBanned)

	if err != nil {
		c.String(http.StatusForbidden, "Invalid key")
		return
	}

	if isBanned == 1 {
		c.String(http.StatusForbidden, "Key banned")
		return
	}

	expireDT, _ := time.Parse("2006-01-02 15:04:05", expireDate)
	if time.Now().After(expireDT) {
		c.String(http.StatusForbidden, "Key expired")
		return
	}

	// 更新最后登录时间
	_, _ = db.Exec("UPDATE keys SET last_login=? WHERE key_id=?", time.Now().Format("2006-01-02 15:04:05"), key)

	c.String(http.StatusOK, "success")
}

// 初始化管理员
func initAdmin(c *gin.Context) {
	appName := c.PostForm("app")
	password := c.PostForm("password")

	if appName == "" || password == "" {
		c.String(http.StatusBadRequest, "Missing parameters")
		return
	}

	var exists int
	err := db.QueryRow("SELECT 1 FROM applications WHERE app_name=?", appName).Scan(&exists)
	if err == nil {
		c.String(http.StatusOK, "App already exists")
		return
	}

	_, err = db.Exec("INSERT INTO applications (app_name, password_hash, created_at) VALUES (?, ?, ?)",
		appName, hashPassword(password), time.Now().Format("2006-01-02 15:04:05"))

	if err != nil {
		c.String(http.StatusInternalServerError, "Database error")
		return
	}

	c.String(http.StatusOK, "Admin created successfully")
}

func main() {
	// 初始化数据库
	if err := initDB(); err != nil {
		log.Fatal("Database initialization failed:", err)
	}
	defer db.Close()

	// 配置 Gin
	gin.SetMode(gin.ReleaseMode)
	r := gin.Default()

	// 路由
	r.POST("/admin_login", adminLogin)
	r.POST("/admin", requireAuth(), adminCommand)
	r.POST("/login", clientLogin)
	r.POST("/init_admin", initAdmin)

	// 启动服务器
	log.Println("YumeCloud Protection Auth Server started on :13337")
	if err := r.Run(":13337"); err != nil {
		log.Fatal("Server failed to start:", err)
	}
}
