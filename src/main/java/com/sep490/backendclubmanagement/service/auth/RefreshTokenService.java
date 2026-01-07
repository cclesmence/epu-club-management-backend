package com.sep490.backendclubmanagement.service.auth;

import com.sep490.backendclubmanagement.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Service for managing refresh tokens in Redis with automatic expiration based on JWT token expiry.
 * Uses simple key-value structure: refresh_token:{userId} -> refreshTokenString
 * TTL is set based on the actual JWT token expiration time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Store refresh token for user with custom expiration time
     * @param userId User ID
     * @param refreshToken Refresh token string
     * @param expirationTimeMillis Token expiration time in milliseconds
     */
    public void storeRefreshToken(String userId, String refreshToken, long expirationTimeMillis) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            long currentTime = System.currentTimeMillis();
            long ttlSeconds = Math.max(1, (expirationTimeMillis - currentTime) / 1000);
            
            redisTemplate.opsForValue().set(key, refreshToken, Duration.ofSeconds(ttlSeconds));
            log.debug("Refresh token stored for user: {} with TTL: {}s", userId, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to store refresh token for user: {}", userId, e);
            throw new RuntimeException("Failed to store refresh token", e);
        }
    }

    /**
     * Store refresh token for user (backward compatibility - uses default 1 day)
     * @deprecated Use storeRefreshToken(String userId, String refreshToken, long expirationTimeMillis) instead
     */
    @Deprecated
    public void storeRefreshToken(String userId, String refreshToken) {
        long defaultExpiration = System.currentTimeMillis() + (24 * 60 * 60 * 1000L); // 1 day
        storeRefreshToken(userId, refreshToken, defaultExpiration);
    }

    /**
     * Get refresh token for user
     */
    public Optional<String> getRefreshToken(String userId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            String refreshToken = (String) redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(refreshToken);
        } catch (Exception e) {
            log.error("Failed to get refresh token for user: {}", userId, e);
            return Optional.empty();
        }
    }

    /**
     * Check if user has a valid refresh token
     */
    public boolean hasValidRefreshToken(String userId) {
        return getRefreshToken(userId).isPresent();
    }

    /**
     * Validate refresh token for user
     */
    public boolean isValidRefreshToken(String userId, String refreshToken) {
        try {
            Optional<String> storedToken = getRefreshToken(userId);
            return storedToken.isPresent() && storedToken.get().equals(refreshToken);
        } catch (Exception e) {
            log.error("Failed to validate refresh token for user: {}", userId, e);
            return false;
        }
    }

    /**
     * Revoke refresh token for user
     */
    public void revokeRefreshToken(String userId) {
        try {
            String key = REFRESH_TOKEN_PREFIX + userId;
            redisTemplate.delete(key);
            log.info("Refresh token revoked for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to revoke refresh token for user: {}", userId, e);
        }
    }

    /**
     * Create and store refresh token for user with custom expiration time
     * @param user User entity
     * @param refreshToken Refresh token string
     * @param expirationTimeMillis Token expiration time in milliseconds
     */
    public void createRefreshToken(User user, String refreshToken, long expirationTimeMillis) {
        storeRefreshToken(user.getId().toString(), refreshToken, expirationTimeMillis);
    }

    /**
     * Create and store refresh token for user (backward compatibility)
     * @deprecated Use createRefreshToken(User user, String refreshToken, long expirationTimeMillis) instead
     */
    @Deprecated
    public void createRefreshToken(User user, String refreshToken) {
        storeRefreshToken(user.getId().toString(), refreshToken);
    }



}