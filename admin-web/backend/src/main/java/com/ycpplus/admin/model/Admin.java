package com.ycpplus.admin.model;

public class Admin {
    private String appName;
    private String passwordHash;
    private String createdAt;

    public Admin() {}

    public Admin(String appName, String passwordHash, String createdAt) {
        this.appName = appName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
