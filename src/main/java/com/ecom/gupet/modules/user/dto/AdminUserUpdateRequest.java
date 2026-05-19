package com.ecom.gupet.modules.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class AdminUserUpdateRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 255)
    private String address;

    @Size(max = 20)
    private String phone;

    private String avatarUrl;
    private LocalDate birthday;

    // Admin có thể thay đổi email (cẩn thận)
    private String email;

    // Admin có thể set trực tiếp danh sách role
    private Set<String> roles;

    private boolean changePassword = false;
    private String newPassword;
}