package com.ecom.gupet.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access.expiration}")
    private long accessExpiration;   // ví dụ: 900000 = 15 phút

    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;  // ví dụ: 604800000 = 7 ngày

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(String email) {
        return generateToken(email, accessExpiration);
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, refreshExpiration);
    }

    private String generateToken(String email, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Lấy thời gian hết hạn còn lại của token (đơn vị milliseconds)
     * Dùng để set TTL cho Redis blacklist
     */
    public long getRemainingExpiration(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return 0;
            }

            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);   // không cho giá trị âm
        } catch (Exception e) {
            return 0;   // nếu token invalid thì không blacklist nữa
        }
    }

    /**
     * Lấy expiration time của Refresh Token (dùng khi lưu Redis)*/
    public long getRefreshExpiration() {
        return refreshExpiration;
    }
}