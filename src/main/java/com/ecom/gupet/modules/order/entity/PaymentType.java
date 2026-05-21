package com.ecom.gupet.modules.order.entity;

public enum PaymentType {
    HOLD,             // Giữ tiền khi user đặt hàng
    RELEASE_TO_SHOP,  // Trả tiền shop khi order COMPLETED
    REFUND_TO_BUYER   // Hoàn tiền user khi order CANCELLED
}