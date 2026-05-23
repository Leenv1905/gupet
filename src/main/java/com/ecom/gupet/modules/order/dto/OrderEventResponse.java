package com.ecom.gupet.modules.order.dto;

import com.ecom.gupet.modules.order.entity.OrderEventType;
import com.ecom.gupet.modules.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class OrderEventResponse {
    private Long            id;
    private OrderEventType  eventType;
    private OrderStatus     fromStatus;
    private OrderStatus     toStatus;
    private String          actorRole;   // ROLE_USER | ROLE_SHOP | ROLE_OPERATOR | SYSTEM
    private String          actorName;   // tên người thực hiện (null nếu SYSTEM)
    private Map<String, Object> payload;
    private LocalDateTime   createdAt;
}
