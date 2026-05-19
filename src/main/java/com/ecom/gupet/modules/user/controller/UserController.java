package com.ecom.gupet.modules.user.controller;

import com.ecom.gupet.modules.user.dto.*;
import com.ecom.gupet.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API quản lý thông tin người dùng")
public class UserController {

    private final UserService userService;

    @PutMapping("/profile")
    @Operation(summary = "Cập nhật thông tin cá nhân (có thể thêm role SHOP)")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse response = userService.updateProfileByEmail(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    @Operation(summary = "Lấy thông tin profile của user đang đăng nhập")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserResponse response = userService.getProfileByEmail(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đổi mật khẩu")
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = (Long) SecurityContextHolder.getContext()
                .getAuthentication().getDetails();

        userService.changePassword(userId, request);
        return ResponseEntity.ok("Đổi mật khẩu thành công");
    }

    // ==================== Xem Chi Tiết User (Cho Tất Cả Role) ====================

    @GetMapping("/{userId}")
    @Operation(summary = "Xem chi tiết user (tất cả role đều xem được)")
    public ResponseEntity<UserResponse> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    // ==================== ADMIN API ====================
    @PreAuthorize("hasRole('ADMIN')")   // Chỉ ADMIN mới được gọi
    @PutMapping("/admin/users/{userId}")
    @Operation(summary = "ADMIN - Cập nhật thông tin bất kỳ user nào (bao gồm role, mật khẩu...)")
    public ResponseEntity<UserResponse> adminUpdateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request) {

        UserResponse response = userService.adminUpdateUser(userId, request);
        return ResponseEntity.ok(response);
    }
    // LẤY DANH SÁCH TẤT CẢ USER (ADMIN)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    @Operation(summary = "ADMIN - Lấy danh sách tất cả user")
    public ResponseEntity<List<UserListResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
// ====== XÓA, KHÓAUUSER - TÍNH NĂNG MỞ RỘNG CHO ADMIN ======
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/users/{userId}/status")
    @Operation(summary = "ADMIN - Khóa hoặc xóa user")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusUpdateRequest request) {

        userService.updateUserStatus(userId, request);
        return ResponseEntity.ok().build();
    }

}