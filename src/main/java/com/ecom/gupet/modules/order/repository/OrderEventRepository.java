// ============================================================
// FILE 2: OrderEventRepository.java
// ============================================================
package com.ecom.gupet.modules.order.repository;

import com.ecom.gupet.modules.order.entity.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {

    // Lấy toàn bộ lịch sử sự kiện của một đơn, sắp xếp theo thời gian
    List<OrderEvent> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}