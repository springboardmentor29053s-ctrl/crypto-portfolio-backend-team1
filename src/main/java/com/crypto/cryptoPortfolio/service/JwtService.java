package com.crypto.cryptoPortfolio.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT Service - Complete Implementation
 *
 * Features:
 * ✅ Generate access tokens (1 hour expiry)
 * ✅ Generate refresh tokens (7 days expiry)
 * ✅ Validate tokens
 * ✅ Extract email from token
 * ✅ Extract user ID from token
 * ✅ Refresh access token using refresh token
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:3600000}")  // 1 hour (in milliseconds)
    private long expiration;

    @Value("${jwt.refresh.expiration:604800000}")  // 7 days (in milliseconds)
    private long refreshExpiration;

    // ═════════════════════════════════════════════════════════════════════════
    // Helper: Get signing key
    // ═════════════════════════════════════════════════════════════════════════
    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1️⃣ GENERATE ACCESS TOKEN (1 hour)
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Generate a JWT access token for a user
     *
     * @param email User's email address
     * @return JWT token string
     *
     * Example:
     *   String token = jwtService.generateToken("user@example.com");
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 2️⃣ GENERATE TOKEN WITH USER ID
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Generate JWT token with user ID claim
     *
     * @param email User's email
     * @param userId User's unique ID
     * @return JWT token string
     */
    public String generateTokenWithUserId(String email, Long userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 3️⃣ GENERATE REFRESH TOKEN (7 days)
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Generate a refresh token with extended expiry (7 days)
     * Used to get new access tokens without re-authentication
     *
     * @param email User's email
     * @return Refresh token string
     *
     * Example:
     *   String refreshToken = jwtService.generateRefreshToken("user@example.com");
     */
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 4️⃣ GENERATE REFRESH TOKEN WITH USER ID
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Generate refresh token with user ID
     */
    public String generateRefreshTokenWithUserId(String email, Long userId) {
        return Jwts.builder()
                .setSubject(email)
                .claim("userId", userId)
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 5️⃣ EXTRACT EMAIL FROM TOKEN
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Extract email (subject) from JWT token
     *
     * @param token JWT token string
     * @return Email address
     * @throws JwtException if token is invalid
     *
     * Example:
     *   String email = jwtService.extractEmail(token);
     */
    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 6️⃣ EXTRACT USER ID FROM TOKEN
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Extract user ID claim from JWT token
     *
     * @param token JWT token string
     * @return User ID
     * @throws JwtException if token is invalid or has no userId claim
     *
     * Example:
     *   Long userId = jwtService.getUserIdFromToken(token);
     */
    public Long getUserIdFromToken(String token) {
        try {
            Object userId = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .get("userId");

            if (userId == null) {
                throw new JwtException("userId claim not found in token");
            }

            // Handle both Integer and Long
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            } else if (userId instanceof Long) {
                return (Long) userId;
            } else {
                return Long.valueOf(userId.toString());
            }
        } catch (Exception e) {
            throw new JwtException("Cannot extract userId from token: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 7️⃣ VALIDATE TOKEN
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Validate JWT token
     * Checks signature and expiration
     *
     * @param token JWT token string
     * @return true if valid, false otherwise
     *
     * Example:
     *   boolean isValid = jwtService.validateToken(token);
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("❌ Token expired: " + e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            System.err.println("❌ Unsupported JWT: " + e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            System.err.println("❌ Malformed JWT: " + e.getMessage());
            return false;
        } catch (SignatureException e) {
            System.err.println("❌ Invalid signature: " + e.getMessage());
            return false;
        } catch (JwtException e) {
            System.err.println("❌ JWT validation failed: " + e.getMessage());
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 8️⃣ REFRESH ACCESS TOKEN
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Generate new access token using refresh token
     *
     * @param refreshToken Valid refresh token
     * @return New access token
     * @throws JwtException if refresh token is invalid
     *
     * Example:
     *   String newAccessToken = jwtService.refreshAccessToken(refreshToken);
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            // Validate refresh token
            if (!validateToken(refreshToken)) {
                throw new JwtException("Invalid or expired refresh token");
            }

            // Extract email from refresh token
            String email = extractEmail(refreshToken);

            // Check if it's actually a refresh token
            Object tokenType = Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody()
                    .get("type");

            if (!"refresh".equals(tokenType)) {
                throw new JwtException("Token is not a refresh token");
            }

            // Generate new access token
            return generateToken(email);

        } catch (JwtException e) {
            throw new JwtException("Token refresh failed: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 9️⃣ REFRESH ACCESS TOKEN WITH USER ID
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Refresh token and include user ID claim
     */
    public String refreshAccessTokenWithUserId(String refreshToken) {
        try {
            if (!validateToken(refreshToken)) {
                throw new JwtException("Invalid or expired refresh token");
            }

            String email = extractEmail(refreshToken);
            Long userId = getUserIdFromToken(refreshToken);

            return generateTokenWithUserId(email, userId);

        } catch (Exception e) {
            throw new JwtException("Token refresh failed: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 🔟 GET TOKEN EXPIRATION TIME
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Get token expiration date
     *
     * @param token JWT token
     * @return Expiration date
     */
    public Date getExpirationDate(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
        } catch (Exception e) {
            throw new JwtException("Cannot extract expiration from token");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // 1️⃣1️⃣ CHECK IF TOKEN IS EXPIRED
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            return getExpirationDate(token).before(new Date());
        } catch (Exception e) {
            return true; // Treat as expired if we can't read it
        }
    }
}