package com.ycpplus.admin.model;

import java.time.LocalDateTime;

public class LicenseKey {
    private String key;
    private String appName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastLogin;
    private boolean banned;
    private int loginCount;

    public LicenseKey() {}

    public LicenseKey(String key, String appName, LocalDateTime createdAt, LocalDateTime expiresAt,
                      LocalDateTime lastLogin, boolean banned, int loginCount) {
        this.key = key;
        this.appName = appName;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastLogin = lastLogin;
        this.banned = banned;
        this.loginCount = loginCount;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public boolean isBanned() { return banned; }
    public void setBanned(boolean banned) { this.banned = banned; }

    public int getLoginCount() { return loginCount; }
    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !banned && !isExpired();
    }

    public long getDaysUntilExpiry() {
        if (expiresAt == null) return -1;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }
}
