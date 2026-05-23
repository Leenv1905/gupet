package com.ecom.gupet.modules.order.controller;

import com.ecom.gupet.modules.order.dto.*;
import com.ecom.gupet.modules.order.entity.OrderStatus;
import com.ecom.gupet.modules.order.service.OrderService;
import com.ecom.gupet.modules.order.service.OrderEventService;
import com.ecom.gupet.modules.order.dto.OrderEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "API quản lý đơn hàng thú cưng")
public class OrderController {

    private final OrderService orderService;
    private final OrderEventService orderEventService;

    // =========================================================================
    // USER — ĐẶT HÀNG VÀ THEO DÕI ĐƠN
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "USER — Đặt hàng từ giỏ hàng + thanh toán")
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    @GetMapping("/my-purchases")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "USER — Danh sách đơn tôi đã mua")
    public ResponseEntity<List<OrderSummaryResponse>> getMyPurchases() {
        return ResponseEntity.ok(orderService.getMyOrdersAsBuyer());
    }

    @PutMapping("/{orderId}/confirm-received")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "USER — Xác nhận đã nhận được hàng")
    public ResponseEntity<OrderResponse> confirmReceived(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.customerConfirmReceived(orderId));
    }

    // =========================================================================
    // SHOP — XÁC NHẬN ĐƠN VÀ THEO DÕI
    // =========================================================================

    @GetMapping("/my-sales")
    @PreAuthorize("hasRole('SHOP')")
    @Operation(summary = "SHOP — Danh sách đơn hàng của shop tôi")
    public ResponseEntity<List<OrderSummaryResponse>> getMySales() {
        return ResponseEntity.ok(orderService.getMyOrdersAsSeller());
    }

    @PutMapping("/{orderId}/shop-confirm")
    @PreAuthorize("hasRole('SHOP')")
    @Operation(summary = "SHOP — Xác nhận có hàng để giao")
    public ResponseEntity<OrderResponse> shopConfirm(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.shopConfirm(orderId));
    }

    // =========================================================================
    // OPERATOR — QUẢN LÝ KHO VÀ GIAO HÀNG
    // =========================================================================

    @GetMapping("/operator/pending")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "OPERATOR — Danh sách đơn chờ nhận từ shop")
    public ResponseEntity<List<OrderSummaryResponse>> getPendingAtWarehouse() {
        return ResponseEntity.ok(orderService.getOrdersByStatus(OrderStatus.SHOP_CONFIRMED));
    }

    @GetMapping("/operator/inspecting")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "OPERATOR — Danh sách đơn đang kiểm tra")
    public ResponseEntity<List<OrderSummaryResponse>> getInspecting() {
        return ResponseEntity.ok(orderService.getOrdersByStatus(OrderStatus.WAREHOUSE_RECEIVED));
    }

    @GetMapping("/operator/delivering")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "OPERATOR — Danh sách đơn đang giao")
    public ResponseEntity<List<OrderSummaryResponse>> getDelivering() {
        return ResponseEntity.ok(orderService.getOrdersByStatus(OrderStatus.OUT_FOR_DELIVERY));
    }

    @PutMapping("/{orderId}/warehouse-receive")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "OPERATOR — Xác nhận hàng đã đến kho, bắt đầu kiểm tra")
    public ResponseEntity<OrderResponse> warehouseReceive(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.warehouseReceive(orderId));
    }

    @PutMapping("/{orderId}/inspection")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "OPERATOR — Gửi kết quả kiểm tra (PASSED / FAILED)")
    public ResponseEntity<OrderResponse> submitInspection(
            @PathVariable Long orderId,
            @Valid @RequestBody InspectionRequest request) {
        return ResponseEntity.ok(orderService.submitInspection(orderId, request));
    }

    @PutMapping("/{orderId}/start-delivery")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "OPERATOR — Bắt đầu giao hàng đến khách")
    public ResponseEntity<OrderResponse> startDelivery(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.startDelivery(orderId));
    }

    @PutMapping("/{orderId}/delivery-failed")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "OPERATOR — Báo giao thất bại (kèm retry hoặc hủy)")
    public ResponseEntity<OrderResponse> reportDeliveryFailed(
            @PathVariable Long orderId,
            @Valid @RequestBody DeliveryFailedRequest request) {
        return ResponseEntity.ok(orderService.reportDeliveryFailed(orderId, request));
    }

    @PutMapping("/{orderId}/confirm-delivered")
    @PreAuthorize("hasRole('OPERATOR')")
    @Operation(summary = "OPERATOR — Xác nhận đã giao hàng thành công")
    public ResponseEntity<OrderResponse> confirmDelivered(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.confirmDelivered(orderId));
    }

    // =========================================================================
    // CHUNG — XEM CHI TIẾT ĐƠN (buyer / seller / operator / admin)
    // =========================================================================

    @GetMapping("/{orderId}")
    @Operation(summary = "Xem chi tiết đơn hàng (buyer, seller, operator, admin)")
    public ResponseEntity<OrderResponse> getOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetail(orderId));
    }

    // =========================================================================
    // ADMIN — XEM TẤT CẢ ĐƠN THEO TRẠNG THÁI
    // =========================================================================

    @GetMapping("/admin/by-status")
//    @PreAuthorize("hasRole('ADMIN')")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    @Operation(summary = "ADMIN — Lọc đơn hàng theo trạng thái")
    public ResponseEntity<List<OrderSummaryResponse>> getByStatus(
            @RequestParam OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    /**
     * GET /api/orders/{orderId}/events
     * Lấy toàn bộ event history — buyer, seller, operator, admin đều dùng.
     * Dùng để render timeline chính xác trên frontend.
     */
    @GetMapping("/{orderId}/events")
    @Operation(summary = "Lấy lịch sử sự kiện của đơn hàng (timeline)")
    public ResponseEntity<List<OrderEventResponse>> getOrderEvents(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderEventService.getEventsByOrderId(orderId));
    }
}
