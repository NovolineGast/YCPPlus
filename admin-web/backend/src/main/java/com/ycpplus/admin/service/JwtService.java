package com.ycpplus.admin.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    private static final long EXPIRATION_TIME = 86400000; // 24 hours
    private final SecretKey secretKey;

    public JwtService() {
        this.secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    }

    public String generateToken(String appName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("appName", appName);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(appName)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
            .signWith(secretKey)
            .compact();
    }

    public String extractAppName(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean validateToken(String token, String appName) {
        final String extractedAppName = extractAppName(token);
        return (extractedAppName.equals(appName) && !isTokenExpired(token));
    }

    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public long getExpirationTime() {
        return EXPIRATION_TIME / 1000; // seconds
    }
}
