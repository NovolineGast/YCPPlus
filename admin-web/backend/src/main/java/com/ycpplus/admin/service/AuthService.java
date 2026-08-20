package com.ycpplus.admin.service;

import com.ycpplus.admin.dto.LoginResponse;
import com.ycpplus.admin.model.Admin;
import com.ycpplus.admin.repository.DatabaseRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final DatabaseRepository repository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(DatabaseRepository repository, JwtService jwtService) {
        this.repository = repository;
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
        repository.initDatabase();
    }

    public void initAdmin(String appName, String password) {
        Optional<Admin> existing = repository.findAdminByAppName(appName);
        if (existing.isPresent()) {
            throw new IllegalStateException("Admin already exists for app: " + appName);
        }

        String hashedPassword = passwordEncoder.encode(password);
        Admin admin = new Admin(appName, hashedPassword, LocalDateTime.now().toString());
        repository.saveAdmin(admin);
    }

    public LoginResponse login(String appName, String password) {
        Optional<Admin> admin = repository.findAdminByAppName(appName);

        if (admin.isEmpty() || !passwordEncoder.matches(password, admin.get().getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(appName);
        return new LoginResponse(token, appName, jwtService.getExpirationTime());
    }

    public boolean validateToken(String token, String appName) {
        return jwtService.validateToken(token, appName);
    }

    public String extractAppName(String token) {
        return jwtService.extractAppName(token);
    }
}