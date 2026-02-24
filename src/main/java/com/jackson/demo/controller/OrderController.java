package com.jackson.demo.controller;

import com.jackson.demo.dto.response.OrderResponse;
import com.jackson.demo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Checkout cart and create order")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @PostMapping("/customers/{customerId}/orders/checkout")
    public ResponseEntity<OrderResponse> checkout(@PathVariable Long customerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(customerId));
    }

    @Operation(summary = "List customer orders")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessCustomer(#customerId, authentication)")
    @GetMapping("/customers/{customerId}/orders")
    public List<OrderResponse> listCustomerOrders(@PathVariable Long customerId) {
        return orderService.listCustomerOrders(customerId);
    }

    @Operation(summary = "Get order by id")
    @PreAuthorize("hasRole('ADMIN') or @accessControlService.canAccessOrder(#orderId, authentication)")
    @GetMapping("/orders/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }
}
