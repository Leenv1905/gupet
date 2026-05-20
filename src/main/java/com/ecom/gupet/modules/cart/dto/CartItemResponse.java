package com.ecom.gupet.modules.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartItemResponse {
    private Long id;
    private Long petId;
    private String petName;
    private String petImage;
    private BigDecimal price;
    private int quantity;
}
