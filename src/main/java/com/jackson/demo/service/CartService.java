package com.jackson.demo.service;

import com.jackson.demo.dto.request.AddToCartRequest;
import com.jackson.demo.dto.request.UpdateCartItemRequest;
import com.jackson.demo.dto.response.CartResponse;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.CartItem;
import com.jackson.demo.entity.Product;
import com.jackson.demo.exception.BadRequestException;
import com.jackson.demo.exception.ResourceNotFoundException;
import com.jackson.demo.mapper.ApiMapper;
import com.jackson.demo.repository.CartItemRepository;
import com.jackson.demo.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CustomerService customerService;
    private final ProductService productService;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            CustomerService customerService,
            ProductService productService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.customerService = customerService;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long customerId) {
        return ApiMapper.toCartResponse(getOrCreateCart(customerId));
    }

    @Transactional
    public CartResponse addItem(Long customerId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(customerId);
        Product product = productService.findProduct(request.productId());
        if (!product.isActive()) {
            throw new BadRequestException("Inactive product cannot be added to cart");
        }

        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .orElseGet(CartItem::new);

        if (item.getId() == null) {
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(request.quantity());
        } else {
            item.setQuantity(item.getQuantity() + request.quantity());
        }
        item.setUnitPrice(product.getPrice());
        cartItemRepository.save(item);
        return ApiMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse updateItem(Long customerId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
        item.setQuantity(request.quantity());
        cartItemRepository.save(item);
        return ApiMapper.toCartResponse(cart);
    }

    @SuppressWarnings("null")
    @Transactional
    public CartResponse removeItem(Long customerId, Long itemId) {
        Cart cart = getOrCreateCart(customerId);
        CartItem item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
        cartItemRepository.delete(item);
        return ApiMapper.toCartResponse(cart);
    }

    @Transactional
    public void clearCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    @Transactional(readOnly = true)
    public Cart getOrCreateCart(Long customerId) {
        customerService.findCustomer(customerId);
        return cartRepository.findByCustomerId(customerId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setCustomer(customerService.findCustomer(customerId));
            return cartRepository.save(cart);
        });
    }
}
