package com.ycpplus.admin.dto;

public class LoginRequest {
    private String appName;
    private String password;

    public LoginRequest() {}

    public LoginRequest(String appName, String password) {
        this.appName = appName;
        this.password = password;
    }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
