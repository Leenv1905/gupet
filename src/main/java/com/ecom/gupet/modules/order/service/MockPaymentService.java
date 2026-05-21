package com.ecom.gupet.modules.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

// Active khi KHÔNG phải production
// Khi tích hợp cổng thật: tạo VNPayPaymentService implements PaymentService
// với @Profile("production") — không cần sửa OrderService
@Slf4j
@Service
@Profile("!production")
public class MockPaymentService implements PaymentService {

    @Override
    public PaymentResult hold(Long orderId, BigDecimal amount, String method) {
        log.info("[MOCK PAYMENT] HOLD {} — đơn #{} — method: {}", amount, orderId, method);
        return PaymentResult.builder()
                .success(true)
                .gatewayRef("MOCK-HOLD-" + orderId + "-" + shortUuid())
                .message("Mock: giữ tiền thành công")
                .build();
    }

    @Override
    public PaymentResult releaseToShop(Long orderId, BigDecimal amount, Long sellerId) {
        log.info("[MOCK PAYMENT] RELEASE {} — đơn #{} → shop #{}", amount, orderId, sellerId);
        return PaymentResult.builder()
                .success(true)
                .gatewayRef("MOCK-PAYOUT-" + orderId + "-" + shortUuid())
                .message("Mock: chuyển tiền shop thành công")
                .build();
    }

    @Override
    public PaymentResult refundToBuyer(Long orderId, BigDecimal amount, Long buyerId) {
        log.info("[MOCK PAYMENT] REFUND {} — đơn #{} → user #{}", amount, orderId, buyerId);
        return PaymentResult.builder()
                .success(true)
                .gatewayRef("MOCK-REFUND-" + orderId + "-" + shortUuid())
                .message("Mock: hoàn tiền thành công")
                .build();
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
