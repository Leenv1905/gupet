package com.ecom.gupet.modules.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {

    @Size(max = 100)
    private String fullName;

    @Size(max = 255)
    private String address;

    @Size(max = 20)
    private String phone;

    private String avatarUrl;

    private LocalDate birthday;

    // Chỉ cho phép thêm role SHOP (không cho thay đổi các role khác)
    private boolean addShopRole = false;
}