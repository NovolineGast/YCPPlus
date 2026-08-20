package main

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"database/sql"
	"embed"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io/fs"
	"log"
	"math"
	"math/big"
	"mime"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	_ "github.com/mattn/go-sqlite3"
	"github.com/google/uuid"
)

var db *sql.DB

// 前端构建产物（admin-web/frontend npm run build 后拷贝到 web/）
//
//go:embed web
var webFS embed.FS

const dbTimeLayout = "2006-01-02 15:04:05"
const jwtExpiration = 86400 // 24 小时（秒）

var jwtSecret []byte

// 初始化数据库
func initDB() error {
	dbPath := os.Getenv("YCP_DB_PATH")
	if dbPath == "" {
		dbPath = "./ycp_auth.db"
	}

	var err error
	db, err = sql.Open("sqlite3", dbPath)
	if err != nil {
		return err
	}

	// 应用表（管理员账号，Web 面板与原生协议共用）
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
		login_count INTEGER DEFAULT 0,
		FOREIGN KEY (app_name) REFERENCES applications(app_name)
	)`)
	if err != nil {
		return err
	}

	// 旧库迁移：补充 login_count 列（已存在时报错可忽略）
	_, _ = db.Exec("ALTER TABLE keys ADD COLUMN login_count INTEGER DEFAULT 0")

	// 设置表（存 JWT 密钥，保证重启/多实例后 token 仍有效）
	_, err = db.Exec(`CREATE TABLE IF NOT EXISTS settings (
		name TEXT PRIMARY KEY,
		value TEXT NOT NULL
	)`)
	return err
}

// 加载或生成 JWT 密钥（持久化到数据库）
func loadOrCreateJWTSecret() error {
	var secret string
	err := db.QueryRow("SELECT value FROM settings WHERE name='jwt_secret'").Scan(&secret)
	if err == sql.ErrNoRows {
		b := make([]byte, 32)
		if _, err := rand.Read(b); err != nil {
			return err
		}
		secret = hex.EncodeToString(b)
		if _, err := db.Exec("INSERT INTO settings (name, value) VALUES ('jwt_secret', ?)", secret); err != nil {
			return err
		}
	} else if err != nil {
		return err
	}
	jwtSecret = []byte(secret)
	return nil
}

// 密码哈希（与原生协议一致：SHA256）
func hashPassword(password string) string {
	hash := sha256.Sum256([]byte(password))
	return hex.EncodeToString(hash[:])
}

// 生成密钥（原生命令格式：app_uuid）
func generateKey(appName string, days int) (string, string) {
	keyID := fmt.Sprintf("%s_%s", appName, uuid.New().String())
	expireDate := time.Now().AddDate(0, 0, days).Format(dbTimeLayout)
	return keyID, expireDate
}

// ===== JWT (HS256) =====

func b64url(data []byte) string {
	return base64.RawURLEncoding.EncodeToString(data)
}

func generateJWT(appName string) string {
	now := time.Now().Unix()
	header, _ := json.Marshal(map[string]string{"alg": "HS256", "typ": "JWT"})
	payload, _ := json.Marshal(map[string]interface{}{
		"sub": appName,
		"iat": now,
		"exp": now + jwtExpiration,
	})
	signingInput := b64url(header) + "." + b64url(payload)
	mac := hmac.New(sha256.New, jwtSecret)
	mac.Write([]byte(signingInput))
	return signingInput + "." + b64url(mac.Sum(nil))
}

func validateJWT(token string) (string, bool) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return "", false
	}
	signingInput := parts[0] + "." + parts[1]
	mac := hmac.New(sha256.New, jwtSecret)
	mac.Write([]byte(signingInput))
	expected := b64url(mac.Sum(nil))
	if !hmac.Equal([]byte(expected), []byte(parts[2])) {
		return "", false
	}
	payload, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return "", false
	}
	var claims struct {
		Sub string `json:"sub"`
		Exp int64  `json:"exp"`
	}
	if err := json.Unmarshal(payload, &claims); err != nil {
		return "", false
	}
	if time.Now().Unix() >= claims.Exp {
		return "", false
	}
	return claims.Sub, true
}

// JWT 认证中间件（Web 面板 REST API 用）
func requireJWT() gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		token := strings.TrimPrefix(authHeader, "Bearer ")
		appName, ok := validateJWT(token)
		if !ok {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"error": "Invalid or expired token"})
			return
		}
		c.Set("app_name", appName)
		c.Next()
	}
}

// 表单认证中间件（原生协议用）
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

// ===== 原生协议 API =====

// 管理员登录（表单）
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

// 管理命令（表单）
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
				keyID, appName, expireDate, time.Now().Format(dbTimeLayout))
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

		result, err := db.Exec("UPDATE keys SET last_login=NULL, login_count=0 WHERE key_id=? AND app_name=?", parts[1], appName)
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

// 客户端登录验证（C++ Native 层调用）
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

	expireDT, _ := time.Parse(dbTimeLayout, expireDate)
	if time.Now().After(expireDT) {
		c.String(http.StatusForbidden, "Key expired")
		return
	}

	// 更新最后登录时间和登录次数
	_, _ = db.Exec("UPDATE keys SET last_login=?, login_count=login_count+1 WHERE key_id=?", time.Now().Format(dbTimeLayout), key)

	c.String(http.StatusOK, "success")
}

// 初始化管理员（表单）
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
		appName, hashPassword(password), time.Now().Format(dbTimeLayout))

	if err != nil {
		c.String(http.StatusInternalServerError, "Database error")
		return
	}

	c.String(http.StatusOK, "Admin created successfully")
}

// ===== Web 管理面板 REST API (JSON) =====

type loginRequest struct {
	AppName   string `json:"appName"`
	Password  string `json:"password"`
}

// Web 面板：初始化管理员
func apiInit(c *gin.Context) {
	var req loginRequest
	if err := c.ShouldBindJSON(&req); err != nil || req.AppName == "" || req.Password == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Missing parameters"})
		return
	}

	var exists int
	err := db.QueryRow("SELECT 1 FROM applications WHERE app_name=?", req.AppName).Scan(&exists)
	if err == nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Admin already exists for app: " + req.AppName})
		return
	}

	_, err = db.Exec("INSERT INTO applications (app_name, password_hash, created_at) VALUES (?, ?, ?)",
		req.AppName, hashPassword(req.Password), time.Now().Format(dbTimeLayout))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Admin initialized successfully"})
}

// Web 面板：登录，返回 JWT
func apiLogin(c *gin.Context) {
	var req loginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid credentials"})
		return
	}

	var passwordHash string
	err := db.QueryRow("SELECT password_hash FROM applications WHERE app_name=?", req.AppName).Scan(&passwordHash)
	if err != nil || passwordHash != hashPassword(req.Password) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid credentials"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"token":      generateJWT(req.AppName),
		"appName":    req.AppName,
		"expiresIn":  jwtExpiration,
	})
}

// keyInfo 从数据库行读取的密钥信息
type keyInfo struct {
	KeyID      string
	ExpireDate string
	IsBanned   bool
	LastLogin  sql.NullString
	CreatedAt  string
	LoginCount int
}

func fetchKeys(appName string) ([]keyInfo, error) {
	rows, err := db.Query(`SELECT key_id, expire_date, is_banned, last_login, created_at, login_count
		FROM keys WHERE app_name=? ORDER BY created_at DESC`, appName)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var keys []keyInfo
	for rows.Next() {
		var k keyInfo
		var banned int
		if err := rows.Scan(&k.KeyID, &k.ExpireDate, &banned, &k.LastLogin, &k.CreatedAt, &k.LoginCount); err != nil {
			return nil, err
		}
		k.IsBanned = banned == 1
		keys = append(keys, k)
	}
	return keys, rows.Err()
}

// dbTimeToISO: "2006-01-02 15:04:05" → "2006-01-02T15:04:05"（前端 new Date() 可解析）
func dbTimeToISO(s string) string {
	return strings.Replace(s, " ", "T", 1)
}

func (k keyInfo) expired() bool {
	expireDT, err := time.Parse(dbTimeLayout, k.ExpireDate)
	return err == nil && time.Now().After(expireDT)
}

func (k keyInfo) daysUntilExpiry() int64 {
	expireDT, err := time.Parse(dbTimeLayout, k.ExpireDate)
	if err != nil {
		return -1
	}
	return int64(math.Floor(time.Until(expireDT).Hours() / 24))
}

// Web 面板：仪表盘统计
func apiStats(c *gin.Context) {
	appName := c.GetString("app_name")
	keys, err := fetchKeys(appName)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	var stats struct {
		TotalKeys   int64 `json:"totalKeys"`
		ActiveKeys  int64 `json:"activeKeys"`
		ExpiredKeys int64 `json:"expiredKeys"`
		BannedKeys  int64 `json:"bannedKeys"`
		ExpiringSoon int64 `json:"expiringSoon"`
		TotalLogins int64 `json:"totalLogins"`
	}

	for _, k := range keys {
		stats.TotalKeys++
		if k.IsBanned {
			stats.BannedKeys++
		}
		if k.expired() {
			stats.ExpiredKeys++
		}
		if !k.IsBanned && !k.expired() {
			stats.ActiveKeys++
		}
		if d := k.daysUntilExpiry(); d >= 0 && d <= 7 {
			stats.ExpiringSoon++
		}
		stats.TotalLogins += int64(k.LoginCount)
	}

	c.JSON(http.StatusOK, stats)
}

// Web 面板：密钥列表
func apiListKeys(c *gin.Context) {
	appName := c.GetString("app_name")
	keys, err := fetchKeys(appName)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	result := make([]gin.H, 0, len(keys))
	for _, k := range keys {
		status := "active"
		if k.IsBanned {
			status = "banned"
		} else if k.expired() {
			status = "expired"
		}

		var lastLogin interface{}
		if k.LastLogin.Valid {
			lastLogin = dbTimeToISO(k.LastLogin.String)
		}

		result = append(result, gin.H{
			"key":             k.KeyID,
			"status":          status,
			"createdAt":       dbTimeToISO(k.CreatedAt),
			"expiresAt":       dbTimeToISO(k.ExpireDate),
			"lastLogin":       lastLogin,
			"loginCount":      k.LoginCount,
			"daysUntilExpiry": k.daysUntilExpiry(),
		})
	}

	c.JSON(http.StatusOK, result)
}

type generateKeysRequest struct {
	Amount int    `json:"amount"`
	Days   int    `json:"days"`
	Prefix string `json:"prefix"`
}

const keyCharset = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // 排除易混淆字符

func randomKeyPart() string {
	b := make([]byte, 4)
	for i := range b {
		n, err := rand.Int(rand.Reader, big.NewInt(int64(len(keyCharset))))
		if err != nil {
			// 极端情况下退化为时间熵
			b[i] = keyCharset[time.Now().UnixNano()%int64(len(keyCharset))]
			continue
		}
		b[i] = keyCharset[n.Int64()]
	}
	return string(b)
}

// Web 面板：生成密钥（格式 PREFIX-XXXX-XXXX-XXXX）
func apiGenerateKeys(c *gin.Context) {
	appName := c.GetString("app_name")

	var req generateKeysRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request"})
		return
	}

	if req.Amount < 1 || req.Amount > 50 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Amount must be between 1 and 50"})
		return
	}
	if req.Days < 1 || req.Days > 9999 {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Days must be between 1 and 9999"})
		return
	}

	prefix := "YCP"
	if p := strings.ToUpper(strings.TrimSpace(req.Prefix)); p != "" {
		prefix = p
	}

	now := time.Now()
	expireDate := now.AddDate(0, 0, req.Days).Format(dbTimeLayout)

	keys := make([]string, 0, req.Amount)
	for i := 0; i < req.Amount; i++ {
		key := fmt.Sprintf("%s-%s-%s-%s", prefix, randomKeyPart(), randomKeyPart(), randomKeyPart())
		_, err := db.Exec("INSERT INTO keys (key_id, app_name, expire_date, created_at) VALUES (?, ?, ?, ?)",
			key, appName, expireDate, now.Format(dbTimeLayout))
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
			return
		}
		keys = append(keys, key)
	}

	c.JSON(http.StatusOK, gin.H{"keys": keys})
}

// Web 面板：封禁/解封密钥
func apiSetBan(banned bool) gin.HandlerFunc {
	return func(c *gin.Context) {
		appName := c.GetString("app_name")
		key := c.Param("key")

		value := 0
		if banned {
			value = 1
		}
		_, err := db.Exec("UPDATE keys SET is_banned=? WHERE key_id=? AND app_name=?", value, key, appName)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
			return
		}

		if banned {
			c.JSON(http.StatusOK, gin.H{"message": "Key banned"})
		} else {
			c.JSON(http.StatusOK, gin.H{"message": "Key unbanned"})
		}
	}
}

// Web 面板：删除密钥
func apiDeleteKey(c *gin.Context) {
	appName := c.GetString("app_name")
	key := c.Param("key")

	_, err := db.Exec("DELETE FROM keys WHERE key_id=? AND app_name=?", key, appName)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Database error"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Key deleted"})
}

// Web 面板：密钥指纹（SHA256 前 4 字节十六进制）
func apiFingerprint(c *gin.Context) {
	key := c.Param("key")
	hash := sha256.Sum256([]byte(key))
	c.JSON(http.StatusOK, gin.H{"fingerprint": strings.ToUpper(hex.EncodeToString(hash[:4]))})
}

// ===== 前端静态文件（SPA）=====

func serveWeb(c *gin.Context) {
	// API 路径未匹配到时返回 404 JSON
	if strings.HasPrefix(c.Request.URL.Path, "/api") {
		c.JSON(http.StatusNotFound, gin.H{"error": "Not found"})
		return
	}

	sub, err := fs.Sub(webFS, "web")
	if err != nil {
		c.String(http.StatusInternalServerError, "web resources missing")
		return
	}

	path := strings.TrimPrefix(c.Request.URL.Path, "/")
	if path == "" {
		path = "index.html"
	}

	// 直接读取文件内容返回，绕过 http.FileServer 对 index.html 的 301 重定向
	data, err := fs.ReadFile(sub, path)
	if err != nil {
		// SPA fallback：任何未知路径回退到 index.html
		path = "index.html"
		if data, err = fs.ReadFile(sub, path); err != nil {
			c.String(http.StatusNotFound, "web resources missing")
			return
		}
	}

	contentType := mime.TypeByExtension(filepath.Ext(path))
	if contentType == "" {
		contentType = http.DetectContentType(data)
	}
	c.Data(http.StatusOK, contentType, data)
}

func main() {
	// 初始化数据库
	if err := initDB(); err != nil {
		log.Fatal("Database initialization failed:", err)
	}
	defer db.Close()

	if err := loadOrCreateJWTSecret(); err != nil {
		log.Fatal("JWT secret initialization failed:", err)
	}

	// 配置 Gin
	gin.SetMode(gin.ReleaseMode)
	r := gin.Default()

	// CORS（同端口部署本不需要，便于开发时前端 dev server 直连）
	r.Use(func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Headers", "Authorization, Content-Type")
		c.Header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		c.Next()
	})

	// 原生协议路由（表单，供 Admin Panel / Native 客户端调用）
	r.POST("/admin_login", adminLogin)
	r.POST("/admin", requireAuth(), adminCommand)
	r.POST("/login", clientLogin)
	r.POST("/init_admin", initAdmin)

	// Web 管理面板 REST API（JSON + JWT）
	api := r.Group("/api")
	{
		api.POST("/init", apiInit)
		api.POST("/login", apiLogin)
		api.GET("/dashboard/stats", requireJWT(), apiStats)
		api.GET("/keys", requireJWT(), apiListKeys)
		api.POST("/keys/generate", requireJWT(), apiGenerateKeys)
		api.POST("/keys/:key/ban", requireJWT(), apiSetBan(true))
		api.POST("/keys/:key/unban", requireJWT(), apiSetBan(false))
		api.DELETE("/keys/:key", requireJWT(), apiDeleteKey)
		api.GET("/keys/:key/fingerprint", apiFingerprint)
	}

	// 前端静态资源（SPA fallback）
	r.NoRoute(serveWeb)

	// 启动服务器
	log.Println("YumeCloud Protection Auth Server started on :13337")
	log.Println("Admin Web Panel: http://localhost:13337/")
	if err := r.Run(":13337"); err != nil {
		log.Fatal("Server failed to start:", err)
	}
}
