package com.ecom.gupet.modules.cart.controller;

import com.ecom.gupet.modules.cart.dto.AddToCartRequest;
import com.ecom.gupet.modules.cart.dto.CartResponse;
import com.ecom.gupet.modules.cart.service.CartService;
import com.ecom.gupet.modules.user.entity.User;
import com.ecom.gupet.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(
            @RequestBody AddToCartRequest request
    ) {

        User user = userService.getCurrentUser();

        cartService.addToCart(
                user.getId(),
                request.getPetId(),
                request.getQuantity()
        );

        return ResponseEntity.ok("Added to cart");
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart() {
        User user = userService.getCurrentUser();
        return ResponseEntity.ok(cartService.getCart(user.getId()));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<?> removeCartItem(@PathVariable Long cartItemId) {
        User user = userService.getCurrentUser();
        cartService.removeCartItem(user.getId(), cartItemId);
        return ResponseEntity.ok("Removed item from cart");
    }
}