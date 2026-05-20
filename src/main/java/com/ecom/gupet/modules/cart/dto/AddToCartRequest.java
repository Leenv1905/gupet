package com.ecom.gupet.modules.cart.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequest {
    private Long petId;
    private Integer quantity;
}