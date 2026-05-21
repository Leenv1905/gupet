// ============================================================
// FILE 2: OrderItemResponse.java
// ============================================================
package com.ecom.gupet.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class OrderItemResponse {
    private Long id;
    private Long petId;
    private Integer quantity;
    private BigDecimal price;
    private Map<String, Object> productSnapshot; // name, thumbnailUrl, breed, chipCode...
}
