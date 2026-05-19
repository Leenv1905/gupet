package com.ecom.gupet.modules.auth.controller;

import com.ecom.gupet.modules.auth.dto.*;
import com.ecom.gupet.modules.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request.getRefreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader("Authorization") String authHeader,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.badRequest().build();
        }

        authService.logout(authHeader, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
}

//package com.ecom.gupet.modules.auth.controller;
//
//import com.ecom.gupet.modules.auth.dto.*;
//import com.ecom.gupet.modules.auth.service.AuthService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final AuthService authService;
//
//    @PostMapping("/register")
//    public AuthResponse register(@RequestBody RegisterRequest request) {
//
//        return authService.register(request);
//
//    }
//
//    @PostMapping("/login")
//    public AuthResponse login(@RequestBody LoginRequest request) {
//
//        return authService.login(request);
//
//    }
//
//    @PostMapping("/logout")
//    public ResponseEntity<Void> logout(
//            @RequestHeader("Authorization") String authHeader,
//            @AuthenticationPrincipal UserDetails userDetails) {   // lấy email từ SecurityContext
//
//        if (userDetails == null) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        String email = userDetails.getUsername();
//        authService.logout(authHeader, email);
//
//        return ResponseEntity.ok().build();
//    }
//
//    @PostMapping("/refresh")
//    public AuthResponse refresh(@RequestBody RefreshTokenRequest request) {
//        return authService.refreshToken(request.getRefreshToken());
//    }
//
//}