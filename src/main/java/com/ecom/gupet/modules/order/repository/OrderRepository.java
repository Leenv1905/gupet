// ============================================================
// FILE 1: OrderRepository.java
// ============================================================
package com.ecom.gupet.modules.order.repository;

import com.ecom.gupet.modules.order.entity.Order;
import com.ecom.gupet.modules.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Buyer xem đơn của mình
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.buyer.id = :buyerId ORDER BY o.createdAt DESC")
    List<Order> findByBuyerId(Long buyerId);

    // Shop xem đơn hàng của shop mình
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.seller.id = :sellerId ORDER BY o.createdAt DESC")
    List<Order> findBySellerId(Long sellerId);

    // Operator xem đơn theo trạng thái (dashboard vận hành)
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    // Lấy chi tiết đơn kèm items và events
    @Query("""
        SELECT DISTINCT o FROM Order o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.pet
        LEFT JOIN FETCH o.buyer
        LEFT JOIN FETCH o.seller
        WHERE o.id = :orderId
    """)
    Optional<Order> findByIdWithDetails(Long orderId);

    // Buyer xem đơn theo trạng thái
    List<Order> findByBuyerIdAndStatusOrderByCreatedAtDesc(Long buyerId, OrderStatus status);

    // Shop xem đơn theo trạng thái
    List<Order> findBySellerIdAndStatusOrderByCreatedAtDesc(Long sellerId, OrderStatus status);
}