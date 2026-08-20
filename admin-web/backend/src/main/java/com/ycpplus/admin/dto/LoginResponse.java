package com.ycpplus.admin.dto;

public class LoginResponse {
    private String token;
    private String appName;
    private long expiresIn;

    public LoginResponse() {}

    public LoginResponse(String token, String appName, long expiresIn) {
        this.token = token;
        this.appName = appName;
        this.expiresIn = expiresIn;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}
