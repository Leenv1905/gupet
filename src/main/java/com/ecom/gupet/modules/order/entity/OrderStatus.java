package com.ecom.gupet.modules.order.entity;

public enum OrderStatus {
    PENDING_CONFIRMATION,   // Bước 1: user đặt hàng, chờ shop xác nhận
    SHOP_CONFIRMED,         // Bước 2: shop xác nhận có hàng
    WAREHOUSE_RECEIVED,     // Bước 3: hàng đến kho operator, đang kiểm tra
    PROCESSING_ACCEPTED,    // Bước 4: operator kiểm tra OK, tiếp nhận đơn
    OUT_FOR_DELIVERY,       // Bước 5: operator đang giao hàng
    DELIVERY_FAILED,        // Bước 5b: giao thất bại (khách không nhận)
    DELIVERED,              // Bước 6: operator xác nhận đã giao xong
    DELIVERY_CONFIRMED,     // Bước 7: khách bấm đã nhận hàng
    COMPLETED,              // Bước 8: hoàn tất, hệ thống đã trả tiền shop
    CANCELLED               // Hủy đơn (sau kiểm tra thất bại / giao thất bại nhiều lần)
}