package com.ycpplus.admin.dto;

import com.ycpplus.admin.model.LicenseKey;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeyResponse {
    private String key;
    private String status;
    private String createdAt;
    private String expiresAt;
    private String lastLogin;
    private int loginCount;
    private long daysUntilExpiry;

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
