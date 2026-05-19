package com.ecom.gupet.modules.auth.service;

import com.ecom.gupet.modules.auth.dto.*;
import com.ecom.gupet.modules.user.entity.Role;
import com.ecom.gupet.modules.user.entity.User;
import com.ecom.gupet.modules.user.repository.RoleRepository;
import com.ecom.gupet.modules.user.repository.UserRepository;
import com.ecom.gupet.security.jwt.JwtService;
import com.ecom.gupet.security.jwt.RedisTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Role roleUser = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = Role.builder().name("ROLE_USER").build();
                    return roleRepository.save(newRole);
                });

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone() != null ? request.getPhone() : "")
                .roles(Set.of(roleUser))
                .build();

        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        redisTokenService.saveRefreshToken(user.getEmail(), refreshToken, jwtService.getRefreshExpiration());

        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        redisTokenService.saveRefreshToken(user.getEmail(), refreshToken, jwtService.getRefreshExpiration());

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * REFRESH TOKEN - ĐÃ SỬA THEO JwtService CỦA BẠN
     */
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RuntimeException("Refresh token is required");
        }

        // Sử dụng method có sẵn trong JwtService của bạn
        String email = jwtService.getEmailFromToken(refreshToken);
        if (email == null) {
            throw new RuntimeException("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra refresh token có đúng trong Redis không
        String storedRefreshToken = redisTokenService.getRefreshToken(email);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh token has expired or been revoked");
        }

        // Tạo token mới
        String newAccessToken = jwtService.generateAccessToken(email);
        String newRefreshToken = jwtService.generateRefreshToken(email);

        // Xóa refresh token cũ và lưu token mới
        redisTokenService.deleteRefreshToken(email);
        redisTokenService.saveRefreshToken(email, newRefreshToken, jwtService.getRefreshExpiration());

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    /**
     * Logout
     */
    public void logout(String accessToken, String email) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        long remainingTime = jwtService.getRemainingExpiration(accessToken);

        if (remainingTime > 0) {
            redisTokenService.blacklistToken(accessToken, remainingTime);
        }

        redisTokenService.deleteRefreshToken(email);
    }
}

//package com.ecom.gupet.modules.auth.service;
//
//import com.ecom.gupet.modules.auth.dto.*;
//import com.ecom.gupet.modules.user.entity.Role;
//import com.ecom.gupet.modules.user.entity.User;
//import com.ecom.gupet.modules.user.repository.RoleRepository;
//import com.ecom.gupet.modules.user.repository.UserRepository;
//import com.ecom.gupet.security.jwt.JwtService;
//import com.ecom.gupet.security.jwt.RedisTokenService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Set;
//
//@Service
//@RequiredArgsConstructor
//public class AuthService {
//
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final JwtService jwtService;
//    private final RedisTokenService redisTokenService;
//
//    @Transactional
//    public AuthResponse register(RegisterRequest request) {
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new RuntimeException("Email already exists");
//        }
//
//// Tự động lấy hoặc tạo ROLE_USER
//        Role roleUser = roleRepository.findByName("ROLE_USER")
//                .orElseGet(() -> {
//                    Role newRole = Role.builder()
//                            .name("ROLE_USER")
//                            .build();
//                    return roleRepository.save(newRole);
//                });
//
//        User user = User.builder()
//                .email(request.getEmail())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .fullName(request.getFullName())
//                .phone(request.getPhone() != null ? request.getPhone() : "")
//                .roles(Set.of(roleUser))
//                .build();
//
//        userRepository.save(user);
//
//        String accessToken = jwtService.generateAccessToken(user.getEmail());
//        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
//
//        // Lưu refresh token vào Redis
//        redisTokenService.saveRefreshToken(user.getEmail(), refreshToken, jwtService.getRefreshExpiration()); // cần thêm getter hoặc inject
//
//        return new AuthResponse(accessToken, refreshToken);
//    }
//
//    public AuthResponse login(LoginRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            throw new RuntimeException("Invalid password");
//        }
//
//        String accessToken = jwtService.generateAccessToken(user.getEmail());
//        String refreshToken = jwtService.generateRefreshToken(user.getEmail());
//
//        redisTokenService.saveRefreshToken(user.getEmail(), refreshToken, jwtService.getRefreshExpiration());
//
//        return new AuthResponse(accessToken, refreshToken);
//    }
//
//    // Refresh Token endpoint sẽ làm sau
//    public AuthResponse refreshToken(String refreshToken) {
//        // Logic refresh token sẽ bổ sung ở bước sau
//        throw new UnsupportedOperationException("Refresh token chưa implement");
//    }
//
//    /**
//     * Logout: Blacklist access token + xóa refresh token
//     */
//    public void logout(String accessToken, String email) {
//        if (accessToken != null && accessToken.startsWith("Bearer ")) {
//            accessToken = accessToken.substring(7);   // bỏ prefix Bearer
//        }
//
//        // Tính thời gian còn lại của access token để set TTL Redis
//        long remainingTime = jwtService.getRemainingExpiration(accessToken);
//
//        if (remainingTime > 0) {
//            redisTokenService.blacklistToken(accessToken, remainingTime);
//        }
//
//        // Xóa refresh token khỏi Redis
//        redisTokenService.deleteRefreshToken(email);
//    }
//}