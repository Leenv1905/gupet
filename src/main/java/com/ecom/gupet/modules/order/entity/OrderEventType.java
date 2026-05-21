package com.ecom.gupet.modules.order.entity;

public enum OrderEventType {
    ORDER_PLACED,         // User đặt hàng
    PAYMENT_CAPTURED,     // Hệ thống giữ tiền thành công
    SHOP_CONFIRMED,       // Shop xác nhận có hàng
    WAREHOUSE_RECEIVED,   // Operator nhận hàng vào kho
    INSPECTION_PASSED,    // Operator kiểm tra OK → tiếp nhận
    INSPECTION_FAILED,    // Operator từ chối → hủy đơn
    DELIVERY_STARTED,     // Operator bắt đầu giao
    DELIVERY_FAILED,      // Giao thất bại
    DELIVERY_RETRY,       // Operator quyết định giao lại
    DELIVERY_COMPLETED,   // Operator xác nhận giao xong
    CUSTOMER_CONFIRMED,   // Khách bấm đã nhận hàng
    PAYOUT_RELEASED,      // Hệ thống chuyển tiền cho shop
    ORDER_CANCELLED       // Đơn bị hủy (kèm lý do)
}