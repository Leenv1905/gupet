package com.ecom.gupet.modules.order.service;

import com.ecom.gupet.modules.cart.entity.Cart;
import com.ecom.gupet.modules.cart.entity.CartItem;
import com.ecom.gupet.modules.cart.repository.CartRepository;
import com.ecom.gupet.modules.order.dto.*;
import com.ecom.gupet.modules.order.entity.*;
import com.ecom.gupet.modules.order.entity.OrderInspection.InspectionResult;
import com.ecom.gupet.modules.order.mapper.OrderMapper;
import com.ecom.gupet.modules.order.repository.*;
import com.ecom.gupet.modules.pet.entity.Pet;
import com.ecom.gupet.modules.pet.entity.PetStatus;
import com.ecom.gupet.modules.pet.repository.PetRepository;
import com.ecom.gupet.modules.user.entity.User;
import com.ecom.gupet.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository               orderRepository;
    private final OrderEventRepository          orderEventRepository;
    private final OrderInspectionRepository     orderInspectionRepository;
    private final PaymentTransactionRepository  paymentTransactionRepository;
    private final CartRepository                cartRepository;
    private final PetRepository                 petRepository;
    private final OrderMapper                   orderMapper;
    private final UserService                   userService;
    private final PaymentService                paymentService; // Mock khi dev, thật khi production

    // =========================================================================
    // BUOC 1: USER DAT HANG + THANH TOAN
    // =========================================================================

    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User buyer = userService.getCurrentUser();

        Cart cart = cartRepository.findByUserId(buyer.getId())
                .orElseThrow(() -> new RuntimeException("Gio hang trong"));

        List<CartItem> selectedItems = cart.getItems().stream()
                .filter(item -> request.getCartItemIds().contains(item.getId()))
                .collect(Collectors.toList());

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Khong tim thay san pham trong gio hang");
        }

        // Validate: tat ca pet phai AVAILABLE va khong phai cua chinh buyer
        for (CartItem cartItem : selectedItems) {
            Pet pet = cartItem.getPet();
            if (pet.getSeller().getId().equals(buyer.getId())) {
                throw new RuntimeException("Khong the mua thu cung cua chinh minh: " + pet.getName());
            }
            if (pet.getStatus() != PetStatus.AVAILABLE) {
                throw new RuntimeException("Thu cung '" + pet.getName() + "' hien khong con kha dung");
            }
        }

        // Validate: tat ca item phai cung 1 seller (1 don = 1 shop)
        Long sellerId = selectedItems.get(0).getPet().getSeller().getId();
        boolean allSameSeller = selectedItems.stream()
                .allMatch(item -> item.getPet().getSeller().getId().equals(sellerId));
        if (!allSameSeller) {
            throw new RuntimeException("Chi co the dat hang tu 1 shop trong moi don");
        }
        User seller = selectedItems.get(0).getPet().getSeller();

        BigDecimal totalAmount = selectedItems.stream()
                .map(item -> item.getPet().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // THANH TOAN: Giu tien
        // Mock: luon SUCCESS. That: goi VNPay/MoMo, cho callback xac nhan.
        PaymentResult holdResult = paymentService.hold(0L, totalAmount, request.getPaymentMethod());
        if (!holdResult.isSuccess()) {
            throw new RuntimeException("Thanh toan that bai: " + holdResult.getMessage());
        }

        // Tao Order
        Order order = Order.builder()
                .buyer(buyer)
                .seller(seller)
                .status(OrderStatus.PENDING_CONFIRMATION)
                .totalAmount(totalAmount)
                .shippingAddress(request.getShippingAddress())
                .shippingPhone(request.getShippingPhone())
                .deliveryAttempts(0)
                .paymentHeldAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .events(new ArrayList<>())
                .paymentTransactions(new ArrayList<>())
                .build();

        // Tao OrderItems + snapshot pet
        for (CartItem cartItem : selectedItems) {
            Pet pet = cartItem.getPet();
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .pet(pet)
                    .quantity(cartItem.getQuantity())
                    .price(pet.getPrice())
                    .productSnapshot(buildPetSnapshot(pet))
                    .build();
            order.getItems().add(orderItem);

            // Chuyen trang thai pet -> RESERVED de khong ai dat trung
            pet.setStatus(PetStatus.RESERVED);
            petRepository.save(pet);
        }

        Order savedOrder = orderRepository.save(order);

        // Luu payment transaction HOLD
        PaymentTransaction holdTx = PaymentTransaction.builder()
                .order(savedOrder)
                .type(PaymentType.HOLD)
                .amount(totalAmount)
                .status(PaymentStatus.SUCCESS)
                .gatewayRef(holdResult.getGatewayRef())
                .note(holdResult.getMessage())
                .build();
        paymentTransactionRepository.save(holdTx);

        // Ghi 2 events: dat hang + giu tien thanh cong
        saveEvent(savedOrder, buyer, OrderEventType.ORDER_PLACED,
                null, OrderStatus.PENDING_CONFIRMATION, "ROLE_USER", Map.of());

        saveEvent(savedOrder, null, OrderEventType.PAYMENT_CAPTURED,
                OrderStatus.PENDING_CONFIRMATION, OrderStatus.PENDING_CONFIRMATION,
                "SYSTEM", Map.of(
                        "amount",      totalAmount,
                        "method",      request.getPaymentMethod(),
                        "gateway_ref", holdResult.getGatewayRef()));

        // Xoa cac item da dat khoi cart
        cart.getItems().removeAll(selectedItems);
        cartRepository.save(cart);

        log.info("Order #{} da duoc tao boi user #{} — da giu tien {}",
                savedOrder.getId(), buyer.getId(), totalAmount);

        return orderMapper.toResponse(
                orderRepository.findByIdWithDetails(savedOrder.getId()).orElseThrow());
    }

    // =========================================================================
    // BUOC 2: SHOP XAC NHAN CO HANG
    // =========================================================================

    @Transactional
    public OrderResponse shopConfirm(Long orderId) {
        User shop = userService.getCurrentUser();
        Order order = getOrderAndValidateStatus(orderId, OrderStatus.PENDING_CONFIRMATION);

        if (!order.getSeller().getId().equals(shop.getId())) {
            throw new RuntimeException("Ban khong co quyen xac nhan don hang nay");
        }

        order.setStatus(OrderStatus.SHOP_CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);

        saveEvent(order, shop, OrderEventType.SHOP_CONFIRMED,
                OrderStatus.PENDING_CONFIRMATION, OrderStatus.SHOP_CONFIRMED,
                "ROLE_SHOP", Map.of());

        log.info("Order #{} da duoc shop #{} xac nhan", orderId, shop.getId());
        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    // =========================================================================
    // BUOC 3: OPERATOR NHAN HANG VAO KHO
    // =========================================================================

    @Transactional
    public OrderResponse warehouseReceive(Long orderId) {
        User operator = userService.getCurrentUser();
        Order order = getOrderAndValidateStatus(orderId, OrderStatus.SHOP_CONFIRMED);

        order.setStatus(OrderStatus.WAREHOUSE_RECEIVED);
        order.setWarehouseReceivedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Tao ban ghi kiem tra voi result = PENDING
        OrderInspection inspection = OrderInspection.builder()
                .order(order)
                .inspector(operator)
                .result(InspectionResult.PENDING)
                .build();
        orderInspectionRepository.save(inspection);

        saveEvent(order, operator, OrderEventType.WAREHOUSE_RECEIVED,
                OrderStatus.SHOP_CONFIRMED, OrderStatus.WAREHOUSE_RECEIVED,
                "ROLE_OPERATOR", Map.of("inspector_id", operator.getId()));

        log.info("Order #{} da den kho — operator #{} bat dau kiem tra", orderId, operator.getId());
        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    // =========================================================================
    // BUOC 4: OPERATOR KIEM TRA — PASSED hoac FAILED
    // =========================================================================

    @Transactional
    public OrderResponse submitInspection(Long orderId, InspectionRequest request) {
        User operator = userService.getCurrentUser();
        Order order = getOrderAndValidateStatus(orderId, OrderStatus.WAREHOUSE_RECEIVED);

        OrderInspection inspection = orderInspectionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException(
                        "Khong tim thay ban ghi kiem tra cho don #" + orderId));

        inspection.setResult(request.getResult());
        inspection.setChecklist(request.getChecklist());
        inspection.setNote(request.getNote());
        inspection.setEvidenceImages(request.getEvidenceImages());
        inspection.setCompletedAt(LocalDateTime.now());

        if (request.getResult() == InspectionResult.PASSED) {
            inspection.setRejectReason(null);
            orderInspectionRepository.save(inspection);

            order.setStatus(OrderStatus.PROCESSING_ACCEPTED);
            order.setAcceptedAt(LocalDateTime.now());
            orderRepository.save(order);

            saveEvent(order, operator, OrderEventType.INSPECTION_PASSED,
                    OrderStatus.WAREHOUSE_RECEIVED, OrderStatus.PROCESSING_ACCEPTED,
                    "ROLE_OPERATOR", Map.of("inspection_id", inspection.getId()));

            log.info("Order #{} kiem tra PASSED — cho giao hang", orderId);

        } else {
            if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
                throw new RuntimeException("Phai nhap ly do tu choi khi kiem tra that bai");
            }
            inspection.setRejectReason(request.getRejectReason());
            orderInspectionRepository.save(inspection);

            // Truyen fromStatus ro rang truoc khi goi cancel
            cancelOrderAndRefund(order, operator, "ROLE_OPERATOR",
                    OrderStatus.WAREHOUSE_RECEIVED,
                    "Kiem tra that bai: " + request.getRejectReason(),
                    Map.of("inspection_id", inspection.getId(),
                            "reject_reason", request.getRejectReason()));

            log.info("Order #{} kiem tra FAILED — da huy va hoan tien", orderId);
        }

        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    // =========================================================================
    // BUOC 5: OPERATOR BAT DAU GIAO HANG
    // =========================================================================

    @Transactional
    public OrderResponse startDelivery(Long orderId) {
        User operator = userService.getCurrentUser();
        Order order = getOrderAndValidateStatus(orderId, OrderStatus.PROCESSING_ACCEPTED);

        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        order.setDeliveryStartedAt(LocalDateTime.now());
        orderRepository.save(order);

        saveEvent(order, operator, OrderEventType.DELIVERY_STARTED,
                OrderStatus.PROCESSING_ACCEPTED, OrderStatus.OUT_FOR_DELIVERY,
                "ROLE_OPERATOR", Map.of());

        log.info("Order #{} bat dau giao hang — operator #{}", orderId, operator.getId());
        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    // =========================================================================
    // BUOC 5b: OPERATOR BAO GIAO THAT BAI — retry hoac huy
    // =========================================================================

    @Transactional
    public OrderResponse reportDeliveryFailed(Long orderId, DeliveryFailedRequest request) {
        User operator = userService.getCurrentUser();
        Order order = getOrderAndValidateStatus(orderId, OrderStatus.OUT_FOR_DELIVERY);

        int currentAttempts = order.getDeliveryAttempts() + 1;
        order.setDeliveryAttempts(currentAttempts);
        order.setDeliveryFailedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.DELIVERY_FAILED);
        orderRepository.save(order);

        saveEvent(order, operator, OrderEventType.DELIVERY_FAILED,
                OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERY_FAILED,
                "ROLE_OPERATOR",
                Map.of("reason",  request.getReason(),
                        "attempt", currentAttempts));

        if (request.isRetry()) {
            order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
            order.setDeliveryStartedAt(LocalDateTime.now());
            orderRepository.save(order);

            saveEvent(order, operator, OrderEventType.DELIVERY_RETRY,
                    OrderStatus.DELIVERY_FAILED, OrderStatus.OUT_FOR_DELIVERY,
                    "ROLE_OPERATOR",
                    Map.of("note",        request.getRetryNote() != null ? request.getRetryNote() : "",
                            "retry_count", currentAttempts));

            log.info("Order #{} giao that bai lan {} — thu giao lai", orderId, currentAttempts);

        } else {
            // fromStatus = DELIVERY_FAILED (trang thai vua set o tren)
            cancelOrderAndRefund(order, operator, "ROLE_OPERATOR",
                    OrderStatus.DELIVERY_FAILED,
                    "Giao hang that bai: " + request.getReason(),
                    Map.of("reason",         request.getReason(),
                            "total_attempts", currentAttempts));

            log.info("Order #{} giao that bai {} lan — da huy va hoan tien", orderId, currentAttempts);
        }

        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    // =========================================================================
    // BUOC 6: OPERATOR XAC NHAN DA GIAO XONG
    // =========================================================================

    @Transactional
    public OrderResponse confirmDelivered(Long orderId) {
        User operator = userService.getCurrentUser();
        Order order = getOrderAndValidateStatus(orderId, OrderStatus.OUT_FOR_DELIVERY);

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        orderRepository.save(order);

        saveEvent(order, operator, OrderEventType.DELIVERY_COMPLETED,
                OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED,
                "ROLE_OPERATOR", Map.of());

        log.info("Order #{} — operator #{} xac nhan da giao xong", orderId, operator.getId());
        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    // =========================================================================
    // BUOC 7: KHACH BAM DA NHAN HANG
    // =========================================================================

    @Transactional
    public OrderResponse customerConfirmReceived(Long orderId) {
        User buyer = userService.getCurrentUser();
        Order order = getOrderAndValidateStatus(orderId, OrderStatus.DELIVERED);

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new RuntimeException("Ban khong co quyen xac nhan don hang nay");
        }

        order.setStatus(OrderStatus.DELIVERY_CONFIRMED);
        order.setDeliveryConfirmedAt(LocalDateTime.now());
        orderRepository.save(order);

        saveEvent(order, buyer, OrderEventType.CUSTOMER_CONFIRMED,
                OrderStatus.DELIVERED, OrderStatus.DELIVERY_CONFIRMED,
                "ROLE_USER", Map.of());

        log.info("Order #{} — khach #{} xac nhan da nhan hang", orderId, buyer.getId());

        // Buoc 8 tu dong kich hoat ngay sau khi khach xac nhan
        completeOrderAndPayout(order);

        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
    }

    // =========================================================================
    // BUOC 8 (TU DONG): HOAN TAT DON — HE THONG CHUYEN TIEN CHO SHOP
    // =========================================================================

    @Transactional
    public void completeOrderAndPayout(Order order) {

        // THANH TOAN: Chuyen tien cho shop
        // Neu that bai: log lai de xu ly thu cong, KHONG rollback order
        // vi khach da nhan hang roi — can retry payment rieng
        PaymentResult payoutResult = paymentService.releaseToShop(
                order.getId(), order.getTotalAmount(), order.getSeller().getId());
        if (!payoutResult.isSuccess()) {
            log.error("PAYOUT FAILED — don #{} — can xu ly thu cong: {}",
                    order.getId(), payoutResult.getMessage());
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setPayoutReleasedAt(LocalDateTime.now());
        orderRepository.save(order);

        PaymentTransaction payoutTx = PaymentTransaction.builder()
                .order(order)
                .type(PaymentType.RELEASE_TO_SHOP)
                .amount(order.getTotalAmount())
                .status(payoutResult.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .gatewayRef(payoutResult.getGatewayRef())
                .note(payoutResult.getMessage())
                .build();
        paymentTransactionRepository.save(payoutTx);

        // Chuyen trang thai tat ca pet trong don -> SOLD
        for (OrderItem item : order.getItems()) {
            Pet pet = item.getPet();
            pet.setStatus(PetStatus.SOLD);
            petRepository.save(pet);
        }

        // Ghi event PAYOUT_RELEASED (actor = null = he thong tu dong)
        saveEvent(order, null, OrderEventType.PAYOUT_RELEASED,
                OrderStatus.DELIVERY_CONFIRMED, OrderStatus.COMPLETED,
                "SYSTEM",
                Map.of("amount",        order.getTotalAmount(),
                        "seller_id",     order.getSeller().getId(),
                        "gateway_ref",   payoutResult.getGatewayRef() != null
                                ? payoutResult.getGatewayRef() : "",
                        "payout_status", payoutResult.isSuccess() ? "SUCCESS" : "FAILED"));

        log.info("Order #{} COMPLETED — chuyen {} cho shop #{} [{}]",
                order.getId(), order.getTotalAmount(),
                order.getSeller().getId(), payoutResult.getGatewayRef());
    }

    // =========================================================================
    // QUERY — XEM DON HANG
    // =========================================================================

    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(Long orderId) {
        User currentUser = userService.getCurrentUser();
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang #" + orderId));

        boolean isBuyer      = order.getBuyer().getId().equals(currentUser.getId());
        boolean isSeller     = order.getSeller().getId().equals(currentUser.getId());
        boolean isPrivileged = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR")
                        || a.getAuthority().equals("ROLE_ADMIN"));

        if (!isBuyer && !isSeller && !isPrivileged) {
            throw new RuntimeException("Ban khong co quyen xem don hang nay");
        }

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getMyOrdersAsBuyer() {
        User buyer = userService.getCurrentUser();
        return orderMapper.toSummaryList(orderRepository.findByBuyerId(buyer.getId()));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getMyOrdersAsSeller() {
        User seller = userService.getCurrentUser();
        return orderMapper.toSummaryList(orderRepository.findBySellerId(seller.getId()));
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> getOrdersByStatus(OrderStatus status) {
        return orderMapper.toSummaryList(
                orderRepository.findByStatusOrderByCreatedAtAsc(status));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Guard condition: validate status hien tai khop voi expectedStatus.
     * Goi truoc moi thao tac chuyen trang thai de tranh race condition.
     */
    private Order getOrderAndValidateStatus(Long orderId, OrderStatus expectedStatus) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay don hang #" + orderId));

        if (order.getStatus() != expectedStatus) {
            throw new RuntimeException(String.format(
                    "Don #%d dang o trang thai [%s], can [%s] de thuc hien thao tac nay",
                    orderId, order.getStatus(), expectedStatus));
        }
        return order;
    }

    /**
     * Ghi 1 event vao order_events.
     * actor = null nghia la he thong tu dong (SYSTEM).
     */
    private void saveEvent(Order order, User actor, OrderEventType eventType,
                           OrderStatus fromStatus, OrderStatus toStatus,
                           String actorRole, Map<String, Object> payload) {
        OrderEvent event = OrderEvent.builder()
                .order(order)
                .actor(actor)
                .eventType(eventType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .actorRole(actorRole)
                .payload(payload != null ? new HashMap<>(payload) : new HashMap<>())
                .build();
        orderEventRepository.save(event);
    }

    /**
     * Huy don + hoan tien + rollback pet ve AVAILABLE.
     * Dung chung cho: INSPECTION_FAILED va DELIVERY_FAILED (khong retry).
     *
     * fromStatus phai truyen vao ro rang tu caller —
     * KHONG doc tu order.getStatus() vi status co the da bi ghi de truoc do.
     */
    private void cancelOrderAndRefund(Order order, User actor, String actorRole,
                                      OrderStatus fromStatus,
                                      String cancelReason,
                                      Map<String, Object> eventPayload) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelReason(cancelReason);
        order.setCancelledAt(LocalDateTime.now());
        orderRepository.save(order);

        // THANH TOAN: Hoan tien cho buyer
        PaymentResult refundResult = paymentService.refundToBuyer(
                order.getId(), order.getTotalAmount(), order.getBuyer().getId());
        if (!refundResult.isSuccess()) {
            log.error("REFUND FAILED — don #{} — can xu ly thu cong: {}",
                    order.getId(), refundResult.getMessage());
        }

        PaymentTransaction refundTx = PaymentTransaction.builder()
                .order(order)
                .type(PaymentType.REFUND_TO_BUYER)
                .amount(order.getTotalAmount())
                .status(refundResult.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .gatewayRef(refundResult.getGatewayRef())
                .note(refundResult.getMessage())
                .build();
        paymentTransactionRepository.save(refundTx);

        // Rollback tat ca pet trong don -> AVAILABLE
        for (OrderItem item : order.getItems()) {
            Pet pet = item.getPet();
            pet.setStatus(PetStatus.AVAILABLE);
            petRepository.save(pet);
        }

        // Ghi event voi fromStatus dung (truyen tu caller, khong doc tu order)
        saveEvent(order, actor, OrderEventType.ORDER_CANCELLED,
                fromStatus, OrderStatus.CANCELLED,
                actorRole, eventPayload);
    }

    /**
     * Tao snapshot pet luc dat hang.
     * Luu vao order_items.product_snapshot de giu nguyen du shop sua thong tin sau.
     */
    private Map<String, Object> buildPetSnapshot(Pet pet) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name",        pet.getName());
        snapshot.put("species",     pet.getSpecies() != null ? pet.getSpecies().name() : null);
        snapshot.put("breed",       pet.getBreed());
        snapshot.put("color",       pet.getColor());
        snapshot.put("gender",      pet.getGender());
        snapshot.put("weight",      pet.getWeight());
        snapshot.put("birthDate",   pet.getBirthDate() != null ? pet.getBirthDate().toString() : null);
        snapshot.put("petChipCode", pet.getPetChipCode());
        snapshot.put("sellerName",  pet.getSeller().getFullName());
        snapshot.put("price",       pet.getPrice());

        if (pet.getImages() != null && !pet.getImages().isEmpty()) {
            var firstImg = pet.getImages().get(0);
            snapshot.put("thumbnailUrl", firstImg.getThumbnailUrl() != null
                    ? firstImg.getThumbnailUrl()
                    : firstImg.getImageUrl());
        }
        return snapshot;
    }
}



//package com.ecom.gupet.modules.order.service;
//
//import com.ecom.gupet.modules.cart.entity.Cart;
//import com.ecom.gupet.modules.cart.entity.CartItem;
//import com.ecom.gupet.modules.cart.repository.CartRepository;
//import com.ecom.gupet.modules.order.dto.*;
//import com.ecom.gupet.modules.order.entity.*;
//import com.ecom.gupet.modules.order.entity.OrderInspection.InspectionResult;
//import com.ecom.gupet.modules.order.mapper.OrderMapper;
//import com.ecom.gupet.modules.order.repository.*;
//import com.ecom.gupet.modules.pet.entity.Pet;
//import com.ecom.gupet.modules.pet.entity.PetStatus;
//import com.ecom.gupet.modules.pet.repository.PetRepository;
//import com.ecom.gupet.modules.user.entity.User;
//import com.ecom.gupet.modules.user.service.UserService;
//import lombok.Builder;
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OrderService {
//
//    private final OrderRepository            orderRepository;
//    private final OrderEventRepository       orderEventRepository;
//    private final OrderInspectionRepository  orderInspectionRepository;
//    private final PaymentTransactionRepository paymentTransactionRepository;
//    private final CartRepository             cartRepository;
//    private final PetRepository              petRepository;
//    private final OrderMapper                orderMapper;
//    private final UserService                userService;
//
//    // =========================================================================
//    // BƯỚC 1: USER ĐẶT HÀNG + THANH TOÁN
//    // =========================================================================
//
//    @Transactional
//    public OrderResponse placeOrder(PlaceOrderRequest request) {
//        User buyer = userService.getCurrentUser();
//
//        // Lấy cart của buyer
//        Cart cart = cartRepository.findByUserId(buyer.getId())
//                .orElseThrow(() -> new RuntimeException("Giỏ hàng trống"));
//
//        // Lọc các cartItem được chọn
//        List<CartItem> selectedItems = cart.getItems().stream()
//                .filter(item -> request.getCartItemIds().contains(item.getId()))
//                .collect(Collectors.toList());
//
//        if (selectedItems.isEmpty()) {
//            throw new RuntimeException("Không tìm thấy sản phẩm trong giỏ hàng");
//        }
//
//        // Validate: tất cả pet phải AVAILABLE, không phải của chính buyer
//        for (CartItem cartItem : selectedItems) {
//            Pet pet = cartItem.getPet();
//            if (pet.getSeller().getId().equals(buyer.getId())) {
//                throw new RuntimeException("Không thể mua thú cưng của chính mình: " + pet.getName());
//            }
//            if (pet.getStatus() != PetStatus.AVAILABLE) {
//                throw new RuntimeException("Thú cưng '" + pet.getName() + "' hiện không còn khả dụng");
//            }
//        }
//
//        // Vì mỗi pet là 1 cá thể duy nhất (có chip), validate tất cả item
//        // phải cùng 1 seller (1 đơn = 1 shop)
//        // Nếu muốn hỗ trợ multi-shop thì tách thành nhiều order ở đây
//        Long sellerId = selectedItems.get(0).getPet().getSeller().getId();
//        boolean allSameSeller = selectedItems.stream()
//                .allMatch(item -> item.getPet().getSeller().getId().equals(sellerId));
//        if (!allSameSeller) {
//            throw new RuntimeException("Chỉ có thể đặt hàng từ 1 shop trong mỗi đơn");
//        }
//        User seller = selectedItems.get(0).getPet().getSeller();
//
//        // Tính tổng tiền
//        BigDecimal totalAmount = selectedItems.stream()
//                .map(item -> item.getPet().getPrice()
//                        .multiply(BigDecimal.valueOf(item.getQuantity())))
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        // Tạo Order
//        Order order = Order.builder()
//                .buyer(buyer)
//                .seller(seller)
//                .status(OrderStatus.PENDING_CONFIRMATION)
//                .totalAmount(totalAmount)
//                .shippingAddress(request.getShippingAddress())
//                .shippingPhone(request.getShippingPhone())
//                .deliveryAttempts(0)
//                .items(new ArrayList<>())
//                .events(new ArrayList<>())
//                .paymentTransactions(new ArrayList<>())
//                .build();
//
//        // Tạo OrderItems + snapshot pet
//        for (CartItem cartItem : selectedItems) {
//            Pet pet = cartItem.getPet();
//            OrderItem orderItem = OrderItem.builder()
//                    .order(order)
//                    .pet(pet)
//                    .quantity(cartItem.getQuantity())
//                    .price(pet.getPrice())
//                    .productSnapshot(buildPetSnapshot(pet))
//                    .build();
//            order.getItems().add(orderItem);
//
//            // Chuyển trạng thái pet → RESERVED
//            pet.setStatus(PetStatus.RESERVED);
//            petRepository.save(pet);
//        }
//
//        Order savedOrder = orderRepository.save(order);
//
//        // Ghi event ORDER_PLACED
//        saveEvent(savedOrder, buyer, OrderEventType.ORDER_PLACED,
//                null, OrderStatus.PENDING_CONFIRMATION, "ROLE_USER", Map.of());
//
//        // Giữ tiền — tạo payment transaction HOLD
//        PaymentTransaction holdTx = PaymentTransaction.builder()
//                .order(savedOrder)
//                .type(PaymentType.HOLD)
//                .amount(totalAmount)
//                .status(PaymentStatus.PENDING) // sẽ chuyển SUCCESS sau khi cổng TT xác nhận
//                .note("Giữ tiền đặt cọc đơn #" + savedOrder.getId())
//                .build();
//        paymentTransactionRepository.save(holdTx);
//
//        // Ghi event PAYMENT_CAPTURED (actor = null = system)
//        savedOrder.setPaymentHeldAt(LocalDateTime.now());
//        orderRepository.save(savedOrder);
//        saveEvent(savedOrder, null, OrderEventType.PAYMENT_CAPTURED,
//                OrderStatus.PENDING_CONFIRMATION, OrderStatus.PENDING_CONFIRMATION,
//                "SYSTEM", Map.of("amount", totalAmount, "method", request.getPaymentMethod()));
//
//        // Xóa các item đã đặt khỏi cart
//        cart.getItems().removeAll(selectedItems);
//        cartRepository.save(cart);
//
//        log.info("Order #{} đã được tạo bởi user #{}", savedOrder.getId(), buyer.getId());
//        return orderMapper.toResponse(orderRepository.findByIdWithDetails(savedOrder.getId()).orElseThrow());
//    }
//
//    // =========================================================================
//    // BƯỚC 2: SHOP XÁC NHẬN CÓ HÀNG
//    // =========================================================================
//
//    @Transactional
//    public OrderResponse shopConfirm(Long orderId) {
//        User shop = userService.getCurrentUser();
//        Order order = getOrderAndValidateStatus(orderId, OrderStatus.PENDING_CONFIRMATION);
//
//        // Chỉ đúng seller mới được confirm
//        if (!order.getSeller().getId().equals(shop.getId())) {
//            throw new RuntimeException("Bạn không có quyền xác nhận đơn hàng này");
//        }
//
//        order.setStatus(OrderStatus.SHOP_CONFIRMED);
//        order.setConfirmedAt(LocalDateTime.now());
//        orderRepository.save(order);
//
//        saveEvent(order, shop, OrderEventType.SHOP_CONFIRMED,
//                OrderStatus.PENDING_CONFIRMATION, OrderStatus.SHOP_CONFIRMED,
//                "ROLE_SHOP", Map.of());
//
//        log.info("Order #{} đã được shop #{} xác nhận", orderId, shop.getId());
//        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
//    }
//
//    // =========================================================================
//    // BƯỚC 3: OPERATOR NHẬN HÀNG VÀO KHO
//    // =========================================================================
//
//    @Transactional
//    public OrderResponse warehouseReceive(Long orderId) {
//        User operator = userService.getCurrentUser();
//        Order order = getOrderAndValidateStatus(orderId, OrderStatus.SHOP_CONFIRMED);
//
//        order.setStatus(OrderStatus.WAREHOUSE_RECEIVED);
//        order.setWarehouseReceivedAt(LocalDateTime.now());
//        orderRepository.save(order);
//
//        // Tạo bản ghi kiểm tra với result = PENDING
//        OrderInspection inspection = OrderInspection.builder()
//                .order(order)
//                .inspector(operator)
//                .result(InspectionResult.PENDING)
//                .build();
//        orderInspectionRepository.save(inspection);
//
//        saveEvent(order, operator, OrderEventType.WAREHOUSE_RECEIVED,
//                OrderStatus.SHOP_CONFIRMED, OrderStatus.WAREHOUSE_RECEIVED,
//                "ROLE_OPERATOR", Map.of("inspector_id", operator.getId()));
//
//        log.info("Order #{} đã đến kho, operator #{} bắt đầu kiểm tra", orderId, operator.getId());
//        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
//    }
//
//    // =========================================================================
//    // BƯỚC 4: OPERATOR KIỂM TRA — PASSED hoặc FAILED
//    // =========================================================================
//
//    @Transactional
//    public OrderResponse submitInspection(Long orderId, InspectionRequest request) {
//        User operator = userService.getCurrentUser();
//        Order order = getOrderAndValidateStatus(orderId, OrderStatus.WAREHOUSE_RECEIVED);
//
//        OrderInspection inspection = orderInspectionRepository.findByOrderId(orderId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi kiểm tra cho đơn #" + orderId));
//
//        // Cập nhật kết quả kiểm tra
//        inspection.setResult(request.getResult());
//        inspection.setChecklist(request.getChecklist());
//        inspection.setNote(request.getNote());
//        inspection.setEvidenceImages(request.getEvidenceImages());
//        inspection.setCompletedAt(LocalDateTime.now());
//
//        if (request.getResult() == InspectionResult.PASSED) {
//            // Kiểm tra OK — tiếp nhận đơn
//            inspection.setRejectReason(null);
//            orderInspectionRepository.save(inspection);
//
//            order.setStatus(OrderStatus.PROCESSING_ACCEPTED);
//            order.setAcceptedAt(LocalDateTime.now());
//            orderRepository.save(order);
//
//            saveEvent(order, operator, OrderEventType.INSPECTION_PASSED,
//                    OrderStatus.WAREHOUSE_RECEIVED, OrderStatus.PROCESSING_ACCEPTED,
//                    "ROLE_OPERATOR", Map.of("inspection_id", inspection.getId()));
//
//            log.info("Order #{} kiểm tra PASSED", orderId);
//
//        } else {
//            // Kiểm tra FAILED — hủy đơn, hoàn tiền
//            if (request.getRejectReason() == null || request.getRejectReason().isBlank()) {
//                throw new RuntimeException("Phải nhập lý do từ chối khi kiểm tra thất bại");
//            }
//            inspection.setRejectReason(request.getRejectReason());
//            orderInspectionRepository.save(inspection);
//
//            cancelOrderAndRefund(order, operator, "ROLE_OPERATOR",
//                    "Kiểm tra thất bại: " + request.getRejectReason(),
//                    Map.of("inspection_id", inspection.getId(),
//                           "reject_reason", request.getRejectReason()));
//
//            log.info("Order #{} kiểm tra FAILED — đã hủy và hoàn tiền", orderId);
//        }
//
//        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
//    }
//
//    // =========================================================================
//    // BƯỚC 5: OPERATOR BẮT ĐẦU GIAO HÀNG
//    // =========================================================================
//
//    @Transactional
//    public OrderResponse startDelivery(Long orderId) {
//        User operator = userService.getCurrentUser();
//        Order order = getOrderAndValidateStatus(orderId, OrderStatus.PROCESSING_ACCEPTED);
//
//        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
//        order.setDeliveryStartedAt(LocalDateTime.now());
//        orderRepository.save(order);
//
//        saveEvent(order, operator, OrderEventType.DELIVERY_STARTED,
//                OrderStatus.PROCESSING_ACCEPTED, OrderStatus.OUT_FOR_DELIVERY,
//                "ROLE_OPERATOR", Map.of());
//
//        log.info("Order #{} bắt đầu giao hàng bởi operator #{}", orderId, operator.getId());
//        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
//    }
//
//    // =========================================================================
//    // BƯỚC 5b: OPERATOR BÁO GIAO THẤT BẠI — retry hoặc hủy
//    // =========================================================================
//
//    @Transactional
//    public OrderResponse reportDeliveryFailed(Long orderId, DeliveryFailedRequest request) {
//        User operator = userService.getCurrentUser();
//        Order order = getOrderAndValidateStatus(orderId, OrderStatus.OUT_FOR_DELIVERY);
//
//        // Ghi nhận thất bại + tăng số lần thử
//        order.setStatus(OrderStatus.DELIVERY_FAILED);
//        order.setDeliveryFailedAt(LocalDateTime.now());
//        order.setDeliveryAttempts(order.getDeliveryAttempts() + 1);
//        orderRepository.save(order);
//
//        saveEvent(order, operator, OrderEventType.DELIVERY_FAILED,
//                OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERY_FAILED,
//                "ROLE_OPERATOR",
//                Map.of("reason", request.getReason(),
//                       "attempt", order.getDeliveryAttempts()));
//
//        if (request.isRetry()) {
//            // Giao lại — quay về OUT_FOR_DELIVERY
//            order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
//            order.setDeliveryStartedAt(LocalDateTime.now());
//            orderRepository.save(order);
//
//            saveEvent(order, operator, OrderEventType.DELIVERY_RETRY,
//                    OrderStatus.DELIVERY_FAILED, OrderStatus.OUT_FOR_DELIVERY,
//                    "ROLE_OPERATOR",
//                    Map.of("note", request.getRetryNote() != null ? request.getRetryNote() : "",
//                           "retry_count", order.getDeliveryAttempts()));
//
//            log.info("Order #{} giao thất bại lần {} — thử giao lại", orderId, order.getDeliveryAttempts());
//        } else {
//            // Không giao lại — hủy đơn, hoàn tiền
//            cancelOrderAndRefund(order, operator, "ROLE_OPERATOR",
//                    "Giao hàng thất bại: " + request.getReason(),
//                    Map.of("reason", request.getReason(),
//                           "total_attempts", order.getDeliveryAttempts()));
//
//            log.info("Order #{} giao thất bại — đã hủy và hoàn tiền", orderId);
//        }
//
//        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
//    }
//
//    // =========================================================================
//    // BƯỚC 6: OPERATOR XÁC NHẬN ĐÃ GIAO XONG
//    // =========================================================================
//
//    @Transactional
//    public OrderResponse confirmDelivered(Long orderId) {
//        User operator = userService.getCurrentUser();
//        Order order = getOrderAndValidateStatus(orderId, OrderStatus.OUT_FOR_DELIVERY);
//
//        order.setStatus(OrderStatus.DELIVERED);
//        order.setDeliveredAt(LocalDateTime.now());
//        orderRepository.save(order);
//
//        saveEvent(order, operator, OrderEventType.DELIVERY_COMPLETED,
//                OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED,
//                "ROLE_OPERATOR", Map.of());
//
//        log.info("Order #{} operator xác nhận đã giao xong", orderId);
//        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
//    }
//
//    // =========================================================================
//    // BƯỚC 7: KHÁCH BẤM ĐÃ NHẬN HÀNG
//    // =========================================================================
//
//    @Transactional
//    public OrderResponse customerConfirmReceived(Long orderId) {
//        User buyer = userService.getCurrentUser();
//        Order order = getOrderAndValidateStatus(orderId, OrderStatus.DELIVERED);
//
//        // Chỉ đúng buyer mới được confirm
//        if (!order.getBuyer().getId().equals(buyer.getId())) {
//            throw new RuntimeException("Bạn không có quyền xác nhận đơn hàng này");
//        }
//
//        order.setStatus(OrderStatus.DELIVERY_CONFIRMED);
//        order.setDeliveryConfirmedAt(LocalDateTime.now());
//        orderRepository.save(order);
//
//        saveEvent(order, buyer, OrderEventType.CUSTOMER_CONFIRMED,
//                OrderStatus.DELIVERED, OrderStatus.DELIVERY_CONFIRMED,
//                "ROLE_USER", Map.of());
//
//        // Tự động hoàn tất đơn và trả tiền shop
//        completeOrderAndPayout(order);
//
//        log.info("Order #{} khách đã xác nhận nhận hàng", orderId);
//        return orderMapper.toResponse(orderRepository.findByIdWithDetails(orderId).orElseThrow());
//    }
//
//    // =========================================================================
//    // BƯỚC 8 (TỰ ĐỘNG): HOÀN TẤT ĐƠN — HỆ THỐNG CHUYỂN TIỀN CHO SHOP
//    // =========================================================================
//
//    @Transactional
//    public void completeOrderAndPayout(Order order) {
//        order.setStatus(OrderStatus.COMPLETED);
//        order.setPayoutReleasedAt(LocalDateTime.now());
//        orderRepository.save(order);
//
//        // Tạo payment transaction RELEASE_TO_SHOP
//        PaymentTransaction payoutTx = PaymentTransaction.builder()
//                .order(order)
//                .type(PaymentType.RELEASE_TO_SHOP)
//                .amount(order.getTotalAmount())
//                .status(PaymentStatus.SUCCESS)
//                .note("Chuyển tiền cho shop — đơn #" + order.getId() + " hoàn tất")
//                .build();
//        paymentTransactionRepository.save(payoutTx);
//
//        // Chuyển trạng thái pet → SOLD
//        for (OrderItem item : order.getItems()) {
//            Pet pet = item.getPet();
//            pet.setStatus(PetStatus.SOLD);
//            petRepository.save(pet);
//        }
//
//        // Ghi event PAYOUT_RELEASED (actor = null = system)
//        saveEvent(order, null, OrderEventType.PAYOUT_RELEASED,
//                OrderStatus.DELIVERY_CONFIRMED, OrderStatus.COMPLETED,
//                "SYSTEM",
//                Map.of("amount", order.getTotalAmount(),
//                       "seller_id", order.getSeller().getId()));
//
//        log.info("Order #{} hoàn tất — đã chuyển {} cho shop #{}",
//                order.getId(), order.getTotalAmount(), order.getSeller().getId());
//    }
//
//    // =========================================================================
//    // QUERY — XEM ĐƠN HÀNG
//    // =========================================================================
//
//    @Transactional(readOnly = true)
//    public OrderResponse getOrderDetail(Long orderId) {
//        User currentUser = userService.getCurrentUser();
//        Order order = orderRepository.findByIdWithDetails(orderId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + orderId));
//
//        // Kiểm tra quyền xem: chỉ buyer, seller, operator, admin mới được xem
//        boolean isBuyer   = order.getBuyer().getId().equals(currentUser.getId());
//        boolean isSeller  = order.getSeller().getId().equals(currentUser.getId());
//        boolean isPrivileged = currentUser.getAuthorities().stream()
//                .anyMatch(a -> a.getAuthority().equals("ROLE_OPERATOR")
//                            || a.getAuthority().equals("ROLE_ADMIN"));
//
//        if (!isBuyer && !isSeller && !isPrivileged) {
//            throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
//        }
//
//        return orderMapper.toResponse(order);
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderSummaryResponse> getMyOrdersAsBuyer() {
//        User buyer = userService.getCurrentUser();
//        return orderMapper.toSummaryList(orderRepository.findByBuyerId(buyer.getId()));
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderSummaryResponse> getMyOrdersAsSeller() {
//        User seller = userService.getCurrentUser();
//        return orderMapper.toSummaryList(orderRepository.findBySellerId(seller.getId()));
//    }
//
//    @Transactional(readOnly = true)
//    public List<OrderSummaryResponse> getOrdersByStatus(OrderStatus status) {
//        return orderMapper.toSummaryList(
//                orderRepository.findByStatusOrderByCreatedAtAsc(status));
//    }
//
//    // =========================================================================
//    // PRIVATE HELPERS
//    // =========================================================================
//
//    // Guard condition: kiểm tra status hiện tại trước khi chuyển
//    // Tránh race condition khi nhiều người cùng thao tác 1 đơn
//    private Order getOrderAndValidateStatus(Long orderId, OrderStatus expectedStatus) {
//        Order order = orderRepository.findByIdWithDetails(orderId)
//                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng #" + orderId));
//
//        if (order.getStatus() != expectedStatus) {
//            throw new RuntimeException(
//                    String.format("Đơn #%d đang ở trạng thái %s, không thể thực hiện thao tác này (cần %s)",
//                            orderId, order.getStatus(), expectedStatus));
//        }
//        return order;
//    }
//
//    // Ghi 1 event vào order_events — dùng chung cho tất cả bước
//    private void saveEvent(Order order, User actor, OrderEventType eventType,
//                           OrderStatus fromStatus, OrderStatus toStatus,
//                           String actorRole, Map<String, Object> payload) {
//        OrderEvent event = OrderEvent.builder()
//                .order(order)
//                .actor(actor)           // null = system
//                .eventType(eventType)
//                .fromStatus(fromStatus)
//                .toStatus(toStatus)
//                .actorRole(actorRole)
//                .payload(payload != null ? payload : new HashMap<>())
//                .build();
//        orderEventRepository.save(event);
//    }
//
//    // Hủy đơn + hoàn tiền + rollback pet status về AVAILABLE
//    // Dùng chung cho: INSPECTION_FAILED và DELIVERY_FAILED (không retry)
//    private void cancelOrderAndRefund(Order order, User actor,
//                                      String actorRole, String cancelReason,
//                                      Map<String, Object> eventPayload) {
//        order.setStatus(OrderStatus.CANCELLED);
//        order.setCancelReason(cancelReason);
//        order.setCancelledAt(LocalDateTime.now());
//        orderRepository.save(order);
//
//        // Hoàn tiền user
//        PaymentTransaction refundTx = PaymentTransaction.builder()
//                .order(order)
//                .type(PaymentType.REFUND_TO_BUYER)
//                .amount(order.getTotalAmount())
//                .status(PaymentStatus.PENDING) // sẽ cập nhật SUCCESS sau khi cổng TT xác nhận
//                .note("Hoàn tiền: " + cancelReason)
//                .build();
//        paymentTransactionRepository.save(refundTx);
//
//        // Rollback pet status về AVAILABLE
//        for (OrderItem item : order.getItems()) {
//            Pet pet = item.getPet();
//            pet.setStatus(PetStatus.AVAILABLE);
//            petRepository.save(pet);
//        }
//
//        // Ghi event ORDER_CANCELLED
//        saveEvent(order, actor, OrderEventType.ORDER_CANCELLED,
//                order.getStatus(), OrderStatus.CANCELLED,
//                actorRole, eventPayload);
//    }
//
//    // Tạo productSnapshot từ Pet entity
//    // Snapshot này được lưu vào order_items.product_snapshot
//    private Map<String, Object> buildPetSnapshot(Pet pet) {
//        Map<String, Object> snapshot = new HashMap<>();
//        snapshot.put("name",        pet.getName());
//        snapshot.put("species",     pet.getSpecies() != null ? pet.getSpecies().name() : null);
//        snapshot.put("breed",       pet.getBreed());
//        snapshot.put("color",       pet.getColor());
//        snapshot.put("gender",      pet.getGender());      // true=Male, false=Female
//        snapshot.put("weight",      pet.getWeight());
//        snapshot.put("birthDate",   pet.getBirthDate() != null ? pet.getBirthDate().toString() : null);
//        snapshot.put("petChipCode", pet.getPetChipCode());
//        snapshot.put("sellerName",  pet.getSeller().getFullName());
//        snapshot.put("price",       pet.getPrice());
//
//        // Lấy ảnh đầu tiên (ưu tiên thumbnail)
//        if (pet.getImages() != null && !pet.getImages().isEmpty()) {
//            var firstImg = pet.getImages().get(0);
//            snapshot.put("thumbnailUrl", firstImg.getThumbnailUrl() != null
//                    ? firstImg.getThumbnailUrl()
//                    : firstImg.getImageUrl());
//        }
//        return snapshot;
//    }
//
//    public static class PaymentService {
//    }
//
//    @Data
//    @Builder
//    public static class PaymentResult {
//        private boolean success;
//        private String  gatewayRef;   // mã giao dịch — mock thì tự sinh UUID
//        private String  message;
//    }
//}
