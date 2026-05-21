// ============================================================
// FILE 3: PaymentTransactionRepository.java
// ============================================================
package com.ecom.gupet.modules.order.repository;

import com.ecom.gupet.modules.order.entity.PaymentTransaction;
import com.ecom.gupet.modules.order.entity.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrderId(Long orderId);

    // Đối soát theo mã giao dịch cổng thanh toán
    Optional<PaymentTransaction> findByGatewayRef(String gatewayRef);

    Optional<PaymentTransaction> findByOrderIdAndType(Long orderId, PaymentType type);
}