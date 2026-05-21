package com.ecom.gupet.modules.order.mapper;

import com.ecom.gupet.modules.order.dto.OrderItemResponse;
import com.ecom.gupet.modules.order.dto.OrderResponse;
import com.ecom.gupet.modules.order.dto.OrderSummaryResponse;
import com.ecom.gupet.modules.order.entity.Order;
import com.ecom.gupet.modules.order.entity.OrderItem;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "buyerId",   source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.fullName")
    @Mapping(target = "sellerId",  source = "seller.id")
    @Mapping(target = "sellerName",source = "seller.fullName")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "items",     source = "items")
    OrderResponse toResponse(Order order);

    @Mapping(target = "buyerName",  source = "buyer.fullName")
    @Mapping(target = "sellerName", source = "seller.fullName")
    @Mapping(target = "itemCount",  expression = "java(order.getItems().size())")
    OrderSummaryResponse toSummary(Order order);

    List<OrderSummaryResponse> toSummaryList(List<Order> orders);

    @Mapping(target = "petId", source = "pet.id")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponseList(List<OrderItem> items);
}
