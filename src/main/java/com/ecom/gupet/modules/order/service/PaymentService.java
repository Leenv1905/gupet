package com.ecom.gupet.modules.order.service;

import java.math.BigDecimal;

public interface PaymentService {

    // Giữ tiền khi user đặt hàng
    PaymentResult hold(Long orderId, BigDecimal amount, String method);

    // Trả tiền cho shop khi order COMPLETED
    PaymentResult releaseToShop(Long orderId, BigDecimal amount, Long sellerId);

    // Hoàn tiền cho user khi order CANCELLED
    PaymentResult refundToBuyer(Long orderId, BigDecimal amount, Long buyerId);
}
