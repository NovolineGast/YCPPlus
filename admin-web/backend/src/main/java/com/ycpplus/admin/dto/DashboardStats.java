package com.ycpplus.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStats {
    private long totalKeys;
    private long activeKeys;
    private long expiredKeys;
    private long bannedKeys;
    private long expiringSoon; // within 7 days
    private long totalLogins;
}
