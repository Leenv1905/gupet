package com.ecom.gupet.modules.order.entity;

import com.ecom.gupet.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // HOLD          → khi đặt hàng (hệ thống giữ tiền)
    // RELEASE_TO_SHOP → khi order COMPLETED (trả tiền shop)
    // REFUND_TO_BUYER → khi order CANCELLED (hoàn tiền user)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    // Mã giao dịch từ cổng thanh toán (VNPay, MoMo...) để đối soát
    private String gatewayRef;

    // Ghi chú: lý do thất bại, hoặc ghi chú nghiệp vụ
    private String note;
}