package com.jackson.demo.repository;
import java.util.UUID;

import com.jackson.demo.entity.CartItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);
    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);
}
