package com.ycpplus.admin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseKey {
    private String key;
    private String appName;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime lastLogin;
    private boolean banned;
    private int loginCount;

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
