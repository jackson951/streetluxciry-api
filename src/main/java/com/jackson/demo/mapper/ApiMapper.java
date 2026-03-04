package com.jackson.demo.mapper;

import com.jackson.demo.dto.response.CartItemResponse;
import com.jackson.demo.dto.response.CartResponse;
import com.jackson.demo.dto.response.CategoryResponse;
import com.jackson.demo.dto.response.CustomerResponse;
import com.jackson.demo.dto.response.OrderItemResponse;
import com.jackson.demo.dto.response.OrderResponse;
import com.jackson.demo.dto.response.ProductResponse;
import com.jackson.demo.entity.Cart;
import com.jackson.demo.entity.CartItem;
import com.jackson.demo.entity.Category;
import com.jackson.demo.entity.Customer;
import com.jackson.demo.entity.CustomerOrder;
import com.jackson.demo.entity.OrderItem;
import com.jackson.demo.entity.Product;
import java.math.BigDecimal;
import java.util.List;

public final class ApiMapper {

    private ApiMapper() {
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }

    public static ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isActive(),
                List.copyOf(product.getImageUrls()),
                toCategoryResponse(product.getCategory()));
    }

    public static CustomerResponse toCustomerResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress());
    }

    public static CartResponse toCartResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(ApiMapper::toCartItemResponse).toList();
        BigDecimal total = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), cart.getCustomer().getId(), items, total);
    }

    public static CartItemResponse toCartItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    public static OrderResponse toOrderResponse(CustomerOrder order) {
        List<OrderItemResponse> items = order.getItems().stream().map(ApiMapper::toOrderItemResponse).toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDeliveryFee(),
                order.getIsDelivery(),
                order.getShippingAddress(),
                order.getCreatedAt(),
                order.getCustomer().getId(),
                items);
    }

    public static OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal());
    }
}
