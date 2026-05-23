package com.ecom.gupet.modules.order.service;

import com.ecom.gupet.modules.order.dto.OrderEventResponse;
import com.ecom.gupet.modules.order.entity.OrderEvent;
import com.ecom.gupet.modules.order.repository.OrderEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderEventService {

    private final OrderEventRepository orderEventRepository;

    @Transactional(readOnly = true)
    public List<OrderEventResponse> getEventsByOrderId(Long orderId) {
        return orderEventRepository
                .findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderEventResponse toResponse(OrderEvent event) {
        return OrderEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .fromStatus(event.getFromStatus())
                .toStatus(event.getToStatus())
                .actorRole(event.getActorRole())
                // actor null = SYSTEM; lấy tên nếu có
                .actorName(event.getActor() != null ? event.getActor().getFullName() : null)
                .payload(event.getPayload())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
