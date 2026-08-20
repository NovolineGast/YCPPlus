package com.ycpplus.admin.controller;

import com.ycpplus.admin.dto.*;
import com.ycpplus.admin.service.AuthService;
import com.ycpplus.admin.service.LicenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AuthService authService;
    private final LicenseService licenseService;

    public AdminController(AuthService authService, LicenseService licenseService) {
        this.authService = authService;
        this.licenseService = licenseService;
    }

    @PostMapping("/init")
    public ResponseEntity<?> initAdmin(@RequestBody LoginRequest request) {
        try {
            authService.initAdmin(request.getAppName(), request.getPassword());
            return ResponseEntity.ok(Map.of("message", "Admin initialized successfully"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request.getAppName(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
        }
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStats> getStats(@RequestHeader("Authorization") String authHeader) {
        String appName = extractAppName(authHeader);
        DashboardStats stats = licenseService.getStats(appName);
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/keys/generate")
    public ResponseEntity<?> generateKeys(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody GenerateKeysRequest request) {
        try {
            String appName = extractAppName(authHeader);
            List<String> keys = licenseService.generateKeys(appName, request.getAmount(), request.getDays());
            return ResponseEntity.ok(Map.of("keys", keys));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/keys")
    public ResponseEntity<List<KeyResponse>> getAllKeys(@RequestHeader("Authorization") String authHeader) {
        String appName = extractAppName(authHeader);
        List<KeyResponse> keys = licenseService.getAllKeys(appName).stream()
            .map(KeyResponse::from)
            .collect(Collectors.toList());
        return ResponseEntity.ok(keys);
    }

    @PostMapping("/keys/{key}/ban")
    public ResponseEntity<?> banKey(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String key) {
        String appName = extractAppName(authHeader);
        licenseService.banKey(key, appName);
        return ResponseEntity.ok(Map.of("message", "Key banned"));
    }

    @PostMapping("/keys/{key}/unban")
    public ResponseEntity<?> unbanKey(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String key) {
        String appName = extractAppName(authHeader);
        licenseService.unbanKey(key, appName);
        return ResponseEntity.ok(Map.of("message", "Key unbanned"));
    }

    @DeleteMapping("/keys/{key}")
    public ResponseEntity<?> deleteKey(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String key) {
        String appName = extractAppName(authHeader);
        licenseService.deleteKey(key, appName);
        return ResponseEntity.ok(Map.of("message", "Key deleted"));
    }

    @GetMapping("/keys/{key}/fingerprint")
    public ResponseEntity<?> getFingerprint(@PathVariable String key) {
        String fingerprint = licenseService.getKeyFingerprint(key);
        return ResponseEntity.ok(Map.of("fingerprint", fingerprint));
    }

    private String extractAppName(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return authService.extractAppName(token);
    }
}
