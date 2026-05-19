package com.ecom.gupet.modules.user.dto;

import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    private boolean locked = false;        // true = khóa tài khoản
    private boolean delete = false;        // true = xóa vĩnh viễn
}