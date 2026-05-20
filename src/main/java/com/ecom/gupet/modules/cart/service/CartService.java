package com.ecom.gupet.modules.cart.service;

import com.ecom.gupet.modules.cart.dto.CartResponse;
import com.ecom.gupet.modules.cart.dto.CartItemResponse;
import com.ecom.gupet.modules.cart.entity.Cart;
import com.ecom.gupet.modules.cart.entity.CartItem;
import com.ecom.gupet.modules.cart.repository.CartItemRepository;
import com.ecom.gupet.modules.cart.repository.CartRepository;
import com.ecom.gupet.modules.pet.entity.Pet;
import com.ecom.gupet.modules.pet.entity.PetStatus;
import com.ecom.gupet.modules.pet.repository.PetRepository;
import com.ecom.gupet.modules.user.entity.User;
import com.ecom.gupet.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addToCart(Long userId, Long petId, int quantity) {

        // 1. Lấy hoặc tạo Cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });

        // 2. Kiểm tra Pet tồn tại
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new IllegalArgumentException("Pet not found"));

        // Kiểm tra không mua pet của chính mình
        if (pet.getSeller().getId().equals(userId)) {           // ← Sửa: seller thay vì getUser()
            throw new IllegalArgumentException("Không thể mua thú cưng của chính mình");
        }

        // 3. Kiểm tra trạng thái Pet
        if (pet.getStatus() != PetStatus.AVAILABLE) {           // ← Sửa: so sánh enum trực tiếp
            throw new IllegalArgumentException("Thú cưng này hiện không còn khả dụng để mua");
        }

        // 4. Kiểm tra item đã tồn tại chưa
        Optional<CartItem> optionalItem = cartItemRepository.findByCartIdAndPetId(cart.getId(), petId);

        if (optionalItem.isPresent()) {
            CartItem item = optionalItem.get();
            int currentQty = item.getQuantity() != null ? item.getQuantity() : 0;
            item.setQuantity(currentQty + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setPet(pet);
            item.setQuantity(quantity);
            item.setStatus("PENDING");

            cartItemRepository.save(item);
        }
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return new CartResponse();
        }

        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setUserId(userId);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<CartItemResponse> itemResponses = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            CartItemResponse itemResponse = new CartItemResponse();
            itemResponse.setId(item.getId());
            itemResponse.setPetId(item.getPet().getId());
            itemResponse.setPetName(item.getPet().getName());
            itemResponse.setPrice(item.getPet().getPrice());
            itemResponse.setQuantity(item.getQuantity());

            // Lấy ảnh đầu tiên (ưu tiên thumbnail nếu có)
            if (item.getPet().getImages() != null && !item.getPet().getImages().isEmpty()) {
                var firstImage = item.getPet().getImages().get(0);
                itemResponse.setPetImage(firstImage.getThumbnailUrl() != null
                        ? firstImage.getThumbnailUrl()
                        : firstImage.getImageUrl());
            }

            itemResponses.add(itemResponse);

            if (item.getPet().getPrice() != null) {
                totalAmount = totalAmount.add(
                        item.getPet().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                );
            }
        }

        response.setItems(itemResponses);
        response.setTotalAmount(totalAmount);
        return response;
    }

    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new RuntimeException("Item does not belong to user's cart");
        }

        cartItemRepository.delete(item);
    }
}