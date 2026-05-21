
// ============================================================
// FILE 3: OrderResponse.java  — trả về cho mọi role
// ============================================================
package com.ecom.gupet.modules.order.dto;

import com.ecom.gupet.modules.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String shippingPhone;
    private String cancelReason;
    private Integer deliveryAttempts;

    // Thông tin buyer và seller
    private Long buyerId;
    private String buyerName;
    private Long sellerId;
    private String sellerName;

    // Sản phẩm trong đơn
    private List<OrderItemResponse> items;

    // Timeline — null nghĩa là mốc đó chưa xảy ra
    private LocalDateTime createdAt;
    private LocalDateTime paymentHeldAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime warehouseReceivedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime deliveryStartedAt;
    private LocalDateTime deliveryFailedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime deliveryConfirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime payoutReleasedAt;
}
