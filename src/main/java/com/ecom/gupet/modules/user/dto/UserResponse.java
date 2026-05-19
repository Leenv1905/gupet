package com.ecom.gupet.modules.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String address;
    private String phone;
    private String avatarUrl;
    private LocalDate birthday;
    private List<String> roles;
}