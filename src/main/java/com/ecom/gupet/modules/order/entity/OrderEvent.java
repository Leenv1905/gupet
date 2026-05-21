package com.ecom.gupet.modules.order.entity;

import com.ecom.gupet.modules.user.entity.User;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

// QUAN TRỌNG: Bảng này là audit log bất biến
// KHÔNG BAO GIỜ UPDATE hoặc DELETE bất kỳ record nào
// Mỗi thay đổi trạng thái = 1 record mới

@Entity
@Table(name = "order_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // NULL nếu event do hệ thống tự động kích hoạt
    // (ví dụ: PAYMENT_CAPTURED, PAYOUT_RELEASED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderEventType eventType;

    // NULL nếu là event đầu tiên (ORDER_PLACED)
    @Enumerated(EnumType.STRING)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus toStatus;

    // Role đang dùng lúc thực hiện: ROLE_USER, ROLE_SHOP, ROLE_OPERATOR, ROLE_ADMIN, SYSTEM
    @Column(length = 20)
    private String actorRole;

    // Dữ liệu bổ sung tùy event:
    // INSPECTION_FAILED  → {"reason": "Sai giống", "inspection_id": 42}
    // DELIVERY_FAILED    → {"reason": "Khách không có nhà", "attempt": 1}
    // DELIVERY_RETRY     → {"note": "Hẹn lại ngày mai", "retry_count": 2}
    // ORDER_CANCELLED    → {"cancelled_by": "operator", "reason": "..."}
    // PAYOUT_RELEASED    → {"amount": 3500000, "gateway_ref": "VNP123"}
    @Type(JsonType.class)
    @Column(columnDefinition = "json", nullable = false)
    private Map<String, Object> payload;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}