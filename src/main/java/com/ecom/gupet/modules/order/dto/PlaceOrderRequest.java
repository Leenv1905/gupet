// ============================================================
// FILE 1: PlaceOrderRequest.java
// ============================================================
package com.ecom.gupet.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class PlaceOrderRequest {

    // Buyer gửi lên danh sách cartItemId muốn đặt
    // (hỗ trợ đặt nhiều item cùng lúc từ cart)
    @NotEmpty(message = "Phải chọn ít nhất 1 sản phẩm")
    private List<Long> cartItemIds;

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(max = 255)
    private String shippingAddress;

    @NotBlank(message = "Số điện thoại giao hàng không được để trống")
    @Size(max = 20)
    private String shippingPhone;

    // Phương thức thanh toán (mở rộng sau: VNPAY, MOMO, COD...)
    private String paymentMethod = "VNPAY";
}

