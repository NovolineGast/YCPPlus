package com.ycpplus.admin.dto;

import com.ycpplus.admin.model.LicenseKey;

public class KeyResponse {
    private String key;
    private String status;
    private String createdAt;
    private String expiresAt;
    private String lastLogin;
    private int loginCount;
    private long daysUntilExpiry;

    public KeyResponse() {}

    public KeyResponse(String key, String status, String createdAt, String expiresAt,
                       String lastLogin, int loginCount, long daysUntilExpiry) {
        this.key = key;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastLogin = lastLogin;
        this.loginCount = loginCount;
        this.daysUntilExpiry = daysUntilExpiry;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }

    public int getLoginCount() { return loginCount; }
    public void setLoginCount(int loginCount) { this.loginCount = loginCount; }

    public long getDaysUntilExpiry() { return daysUntilExpiry; }
    public void setDaysUntilExpiry(long daysUntilExpiry) { this.daysUntilExpiry = daysUntilExpiry; }

    public static KeyResponse from(LicenseKey licenseKey) {
        String status = licenseKey.isBanned() ? "banned" :
                       licenseKey.isExpired() ? "expired" : "active";

        return new KeyResponse(
            licenseKey.getKey(),
            status,
            licenseKey.getCreatedAt() != null ? licenseKey.getCreatedAt().toString() : null,
            licenseKey.getExpiresAt() != null ? licenseKey.getExpiresAt().toString() : null,
            licenseKey.getLastLogin() != null ? licenseKey.getLastLogin().toString() : null,
            licenseKey.getLoginCount(),
            licenseKey.getDaysUntilExpiry()
        );
    }
}
