package com.ecom.gupet.modules.user.service;

import com.ecom.gupet.common.util.SecurityUtils;
import com.ecom.gupet.modules.user.dto.*;
import com.ecom.gupet.modules.user.entity.Role;
import com.ecom.gupet.modules.user.entity.User;
import com.ecom.gupet.modules.user.repository.RoleRepository;
import com.ecom.gupet.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .birthday(user.getBirthday())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cập nhật thông tin
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        // Thêm role SHOP nếu yêu cầu
        if (request.isAddShopRole()) {
            Role shopRole = roleRepository.findByName("ROLE_SHOP")
                    .orElseGet(() -> {
                        Role newRole = Role.builder().name("ROLE_SHOP").build();
                        return roleRepository.save(newRole);
                    });

            user.getRoles().add(shopRole);
        }

        userRepository.save(user);

        return getProfile(userId); // Trả về thông tin sau khi update
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Kiểm tra mật khẩu cũ có đúng không
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }

        // Kiểm tra mật khẩu mới và xác nhận có khớp không
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận không khớp");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
// ==================== LẤY DANH SÁCH USER ====================
    public List<UserListResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> UserListResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .avatarUrl(user.getAvatarUrl())
                        .birthday(user.getBirthday())
                        .roles(user.getRoles().stream().map(Role::getName).toList())
                        .enabled(true)
                        .build())
                .toList();
    }

    // ==================== Xem Chi Tiết User (Cho Tất Cả Role) ====================
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .address(user.getAddress())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .birthday(user.getBirthday())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }
// ==================== FOR ADMIN API ====================
    @Transactional
    public UserResponse adminUpdateUser(Long userId, AdminUserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Cập nhật thông tin cơ bản
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getBirthday() != null) user.setBirthday(request.getBirthday());
        if (request.getEmail() != null) user.setEmail(request.getEmail());

        // Cập nhật roles (Admin set trực tiếp)
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> newRoles = new HashSet<>();
            for (String roleName : request.getRoles()) {
                Role role = roleRepository.findByName(roleName)
                        .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
                newRoles.add(role);
            }
            user.setRoles(newRoles);
        }

        // Đổi mật khẩu (nếu Admin yêu cầu)
        if (request.isChangePassword() && request.getNewPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return getProfile(userId);
    }

    //=========XÓA, KHÓA USER - TÍNH NĂNG MỞ RỘNG=======
    @Transactional
    public void updateUserStatus(Long userId, UserStatusUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.isDelete()) {
            userRepository.delete(user);
        } else if (request.isLocked()) {
            // Tạm thời dùng cách đơn giản: thêm trường enabled vào User sau này
            // Hoặc bạn có thể thêm trường isLocked vào Entity User
            System.out.println("User " + userId + " has been locked");
            // TODO: Thêm logic khóa tài khoản sau
        }
    }

    public UserResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        return convertToResponse(user);
    }

    @Transactional
    public UserResponse updateProfileByEmail(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Cập nhật thông tin
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getBirthday() != null) user.setBirthday(request.getBirthday());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        // Thêm role SHOP
        if (request.isAddShopRole()) {
            Role shopRole = roleRepository.findByName("ROLE_SHOP")
                    .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_SHOP").build()));

            user.getRoles().add(shopRole);
        }

        userRepository.save(user);
        return convertToResponse(user);
    }

    // Helper method
    private UserResponse convertToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .birthday(user.getBirthday())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build();
    }
    // ==================== LẤY USER HIỆN TẠI TỪ JWT ====================
    public User getCurrentUser() {
        String email = SecurityUtils.getCurrentUserEmail();   // Giả sử bạn đang dùng SecurityUtils như trước

        if (email == null) {
            throw new RuntimeException("Unauthorized - No user logged in");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}