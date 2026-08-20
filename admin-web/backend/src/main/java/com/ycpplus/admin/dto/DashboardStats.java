package com.ycpplus.admin.dto;

public class DashboardStats {
    private long totalKeys;
    private long activeKeys;
    private long expiredKeys;
    private long bannedKeys;
    private long expiringSoon;
    private long totalLogins;

    public DashboardStats() {}

    public DashboardStats(long totalKeys, long activeKeys, long expiredKeys,
                          long bannedKeys, long expiringSoon, long totalLogins) {
        this.totalKeys = totalKeys;
        this.activeKeys = activeKeys;
        this.expiredKeys = expiredKeys;
        this.bannedKeys = bannedKeys;
        this.expiringSoon = expiringSoon;
        this.totalLogins = totalLogins;
    }

    public long getTotalKeys() { return totalKeys; }
    public void setTotalKeys(long totalKeys) { this.totalKeys = totalKeys; }

    public long getActiveKeys() { return activeKeys; }
    public void setActiveKeys(long activeKeys) { this.activeKeys = activeKeys; }

    public long getExpiredKeys() { return expiredKeys; }
    public void setExpiredKeys(long expiredKeys) { this.expiredKeys = expiredKeys; }

    public long getBannedKeys() { return bannedKeys; }
    public void setBannedKeys(long bannedKeys) { this.bannedKeys = bannedKeys; }

    public long getExpiringSoon() { return expiringSoon; }
    public void setExpiringSoon(long expiringSoon) { this.expiringSoon = expiringSoon; }

    public long getTotalLogins() { return totalLogins; }
    public void setTotalLogins(long totalLogins) { this.totalLogins = totalLogins; }
}
