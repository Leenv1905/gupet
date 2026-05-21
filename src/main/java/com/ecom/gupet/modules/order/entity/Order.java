package com.ecom.gupet.modules.order.entity;

import com.ecom.gupet.common.entity.BaseEntity;
import com.ecom.gupet.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Người mua
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    // Người bán — snapshot lúc đặt, giúp query dashboard shop
    // không cần join qua order_items → pets
    // CHECK: buyer.id != seller.id (không tự mua hàng mình)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING_CONFIRMATION;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    // Snapshot địa chỉ giao hàng tại thời điểm đặt
    // — không bị ảnh hưởng nếu user cập nhật profile sau
    @Column(nullable = false)
    private String shippingAddress;

    @Column(nullable = false, length = 20)
    private String shippingPhone;

    // Lý do hủy (nếu CANCELLED)
    private String cancelReason;

    // Đếm số lần thử giao — tăng mỗi lần DELIVERY_RETRY
    @Column(nullable = false)
    private Integer deliveryAttempts = 0;

    // ==================== TIMESTAMPS TỪNG MỐC TRẠNG THÁI ====================
    // Dùng để: hiển thị timeline, tính SLA, báo cáo vận hành
    // Điền dần khi status thay đổi, không điền trước

    private LocalDateTime paymentHeldAt;        // hệ thống giữ tiền
    private LocalDateTime confirmedAt;          // shop xác nhận
    private LocalDateTime warehouseReceivedAt;  // hàng đến kho
    private LocalDateTime acceptedAt;           // operator chấp nhận
    private LocalDateTime deliveryStartedAt;    // bắt đầu giao
    private LocalDateTime deliveryFailedAt;     // giao thất bại (lần gần nhất)
    private LocalDateTime deliveredAt;          // operator giao xong
    private LocalDateTime deliveryConfirmedAt;  // khách xác nhận
    private LocalDateTime cancelledAt;          // đơn bị hủy
    private LocalDateTime payoutReleasedAt;     // hệ thống trả tiền shop

    // ==================== RELATIONSHIPS ====================

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<OrderEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<PaymentTransaction> paymentTransactions = new ArrayList<>();
}