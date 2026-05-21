
// ============================================================
// FILE 6: OrderSummaryResponse.java  — Dùng cho danh sách, không kèm items
// ============================================================
package com.ecom.gupet.modules.order.dto;

import com.ecom.gupet.modules.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderSummaryResponse {
    private Long id;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String buyerName;
    private String sellerName;
    private Integer itemCount;
    private LocalDateTime createdAt;
}