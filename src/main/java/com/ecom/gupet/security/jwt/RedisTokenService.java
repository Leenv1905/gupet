package com.ecom.gupet.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private static final String REFRESH_PREFIX = "refresh:token:";

    /**
     * Blacklist access token khi logout
     */
    public void blacklistToken(String token, long expirationInMs) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "blacklisted",
                expirationInMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Kiểm tra token có bị blacklist không
     */
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    /**
     * Lưu Refresh Token vào Redis
     */
    public void saveRefreshToken(String email, String refreshToken, long expirationInMs) {
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + email,
                refreshToken,
                expirationInMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Lấy Refresh Token từ Redis
     */
    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
    }

    /**
     * Xóa Refresh Token (khi logout hoặc refresh token mới)
     */
    public void deleteRefreshToken(String email) {
        redisTemplate.delete(REFRESH_PREFIX + email);
    }
}