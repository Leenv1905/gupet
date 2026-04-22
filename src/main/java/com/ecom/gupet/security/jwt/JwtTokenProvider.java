//package com.ecom.gupet.security.jwt;
//
//import io.jsonwebtoken.Claims;
//import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.security.Keys;
//import org.springframework.stereotype.Component;
//
//import javax.crypto.SecretKey;
//import java.util.Date;
//
//@Component
//public class JwtTokenProvider {
//
//    private static final String SECRET = "gupet-super-secret-key-gupet-super-secret-key";
//
//    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());
//
//    private final long JWT_EXPIRATION = 86400000; // 24h
//
//    public String generateToken(String email) {
//
//        Date now = new Date();
//        Date expiry = new Date(now.getTime() + JWT_EXPIRATION);
//
//        return Jwts.builder()
//                .subject(email)
//                .issuedAt(now)
//                .expiration(expiry)
//                .signWith(key)
//                .compact();
//    }
//
//    public String getEmailFromToken(String token) {
//
//        Claims claims = Jwts.parser()
//                .verifyWith(key)
//                .build()
//                .parseSignedClaims(token)
//                .getPayload();
//
//        return claims.getSubject();
//    }
//
//    public boolean validateToken(String token) {
//
//        try {
//
//            Jwts.parser()
//                    .verifyWith(key)
//                    .build()
//                    .parseSignedClaims(token);
//
//            return true;
//
//        } catch (Exception e) {
//
//            return false;
//
//        }
//    }
//}