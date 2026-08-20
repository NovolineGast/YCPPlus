package com.ycpplus.admin.service;

import com.ycpplus.admin.dto.DashboardStats;
import com.ycpplus.admin.model.LicenseKey;
import com.ycpplus.admin.repository.DatabaseRepository;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class LicenseService {

    private final DatabaseRepository repository;
    private static final String KEY_PREFIX = "YCP-";
    private static final Random random = new Random();

    public LicenseService(DatabaseRepository repository) {
        this.repository = repository;
    }

    public List<String> generateKeys(String appName, int amount, int days) {
        return generateKeys(appName, amount, days, null);
    }

    public List<String> generateKeys(String appName, int amount, int days, String customPrefix) {
        if (amount < 1 || amount > 50) {
            throw new IllegalArgumentException("Amount must be between 1 and 50");
        }
        if (days < 1 || days > 9999) {
            throw new IllegalArgumentException("Days must be between 1 and 9999");
        }

        String prefix = (customPrefix != null && !customPrefix.trim().isEmpty())
            ? customPrefix.trim().toUpperCase() + "-"
            : KEY_PREFIX;

        List<String> keys = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(days);

        for (int i = 0; i < amount; i++) {
            String key = generateUniqueKey(prefix);
            LicenseKey licenseKey = new LicenseKey(
                key, appName, now, expiresAt, null, false, 0
            );
            repository.saveLicenseKey(licenseKey);
            keys.add(key);
        }

        return keys;
    }

    public List<LicenseKey> getAllKeys(String appName) {
        return repository.findAllKeysByAppName(appName);
    }

    public void banKey(String key, String appName) {
        repository.updateKeyBanStatus(key, appName, true);
    }

    public void unbanKey(String key, String appName) {
        repository.updateKeyBanStatus(key, appName, false);
    }

    public void deleteKey(String key, String appName) {
        repository.deleteKey(key, appName);
    }

    public DashboardStats getStats(String appName) {
        List<LicenseKey> allKeys = repository.findAllKeysByAppName(appName);

        long totalKeys = allKeys.size();
        long activeKeys = allKeys.stream().filter(LicenseKey::isActive).count();
        long expiredKeys = allKeys.stream().filter(LicenseKey::isExpired).count();
        long bannedKeys = allKeys.stream().filter(LicenseKey::isBanned).count();
        long expiringSoon = allKeys.stream()
            .filter(k -> k.getDaysUntilExpiry() >= 0 && k.getDaysUntilExpiry() <= 7)
            .count();
        long totalLogins = allKeys.stream().mapToInt(LicenseKey::getLoginCount).sum();

        return new DashboardStats(
            totalKeys, activeKeys, expiredKeys, bannedKeys, expiringSoon, totalLogins
        );
    }

    private String generateUniqueKey(String prefix) {
        // Generate format: PREFIX-XXXX-XXXX-XXXX
        String part1 = generateRandomPart();
        String part2 = generateRandomPart();
        String part3 = generateRandomPart();
        return prefix + part1 + "-" + part2 + "-" + part3;
    }

    private String generateRandomPart() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Exclude confusing chars
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String getKeyFingerprint(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(4, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return "XXXXXXXX";
        }
    }
}
