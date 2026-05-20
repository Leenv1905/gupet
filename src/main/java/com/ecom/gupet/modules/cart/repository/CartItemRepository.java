package com.ecom.gupet.modules.cart.repository;

import com.ecom.gupet.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndPetId(Long cartId, Long petId);
}