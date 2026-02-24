package com.jackson.demo.controller;

import com.jackson.demo.dto.request.AddToCartRequest;
import com.jackson.demo.dto.request.UpdateCartItemRequest;
import com.jackson.demo.dto.response.CartResponse;
import com.jackson.demo.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers/{customerId}/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "Get customer cart")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @GetMapping
    public CartResponse getCart(@PathVariable Long customerId) {
        return cartService.getCart(customerId);
    }

    @Operation(summary = "Add product to cart")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @PostMapping("/items")
    public CartResponse addToCart(@PathVariable Long customerId, @Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(customerId, request);
    }

    @Operation(summary = "Update cart item quantity")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @PatchMapping("/items/{itemId}")
    public CartResponse updateCartItem(
            @PathVariable Long customerId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItem(customerId, itemId, request);
    }

    @Operation(summary = "Remove item from cart")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @DeleteMapping("/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long customerId, @PathVariable Long itemId) {
        return cartService.removeItem(customerId, itemId);
    }

    @Operation(summary = "Clear entire cart")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@PathVariable Long customerId) {
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }
}
