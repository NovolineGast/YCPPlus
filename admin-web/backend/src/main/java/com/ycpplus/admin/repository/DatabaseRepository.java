package com.ycpplus.admin.repository;

import com.ycpplus.admin.model.Admin;
import com.ycpplus.admin.model.LicenseKey;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DatabaseRepository {

    private static final String DB_PATH = "data/ycp_auth.db";

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    public void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create admins table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS admins (
                    app_name TEXT PRIMARY KEY,
                    password_hash TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """);

            // Create keys table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS license_keys (
                    key TEXT PRIMARY KEY,
                    app_name TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    expires_at TEXT,
                    last_login TEXT,
                    banned INTEGER DEFAULT 0,
                    login_count INTEGER DEFAULT 0
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    // Admin operations
    public Optional<Admin> findAdminByAppName(String appName) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM admins WHERE app_name = ?")) {
            stmt.setString(1, appName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new Admin(
                    rs.getString("app_name"),
                    rs.getString("password_hash"),
                    rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public void saveAdmin(Admin admin) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT OR REPLACE INTO admins (app_name, password_hash, created_at) VALUES (?, ?, ?)")) {
            stmt.setString(1, admin.getAppName());
            stmt.setString(2, admin.getPasswordHash());
            stmt.setString(3, admin.getCreatedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // License key operations
    public List<LicenseKey> findAllKeysByAppName(String appName) {
        List<LicenseKey> keys = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM license_keys WHERE app_name = ? ORDER BY created_at DESC")) {
            stmt.setString(1, appName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                keys.add(mapResultSetToKey(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return keys;
    }

    public Optional<LicenseKey> findKeyByKeyAndApp(String key, String appName) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT * FROM license_keys WHERE key = ? AND app_name = ?")) {
            stmt.setString(1, key);
            stmt.setString(2, appName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToKey(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    public void saveLicenseKey(LicenseKey key) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO license_keys (key, app_name, created_at, expires_at, last_login, banned, login_count) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, key.getKey());
            stmt.setString(2, key.getAppName());
            stmt.setString(3, key.getCreatedAt().toString());
            stmt.setString(4, key.getExpiresAt() != null ? key.getExpiresAt().toString() : null);
            stmt.setString(5, key.getLastLogin() != null ? key.getLastLogin().toString() : null);
            stmt.setInt(6, key.isBanned() ? 1 : 0);
            stmt.setInt(7, key.getLoginCount());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateKeyBanStatus(String key, String appName, boolean banned) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "UPDATE license_keys SET banned = ? WHERE key = ? AND app_name = ?")) {
            stmt.setInt(1, banned ? 1 : 0);
            stmt.setString(2, key);
            stmt.setString(3, appName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteKey(String key, String appName) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "DELETE FROM license_keys WHERE key = ? AND app_name = ?")) {
            stmt.setString(1, key);
            stmt.setString(2, appName);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private LicenseKey mapResultSetToKey(ResultSet rs) throws SQLException {
        return new LicenseKey(
            rs.getString("key"),
            rs.getString("app_name"),
            parseDateTime(rs.getString("created_at")),
            parseDateTime(rs.getString("expires_at")),
            parseDateTime(rs.getString("last_login")),
            rs.getInt("banned") == 1,
            rs.getInt("login_count")
        );
    }

    private LocalDateTime parseDateTime(String dateStr) {
        return dateStr != null ? LocalDateTime.parse(dateStr) : null;
    }
}
