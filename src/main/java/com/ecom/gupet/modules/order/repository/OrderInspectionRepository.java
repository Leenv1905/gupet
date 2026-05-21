// ============================================================
// FILE 4: OrderInspectionRepository.java
// ============================================================
package com.ecom.gupet.modules.order.repository;

import com.ecom.gupet.modules.order.entity.OrderInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderInspectionRepository extends JpaRepository<OrderInspection, Long> {

    Optional<OrderInspection> findByOrderId(Long orderId);
}