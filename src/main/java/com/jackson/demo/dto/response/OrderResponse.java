package com.jackson.demo.dto.response;

import com.jackson.demo.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Long customerId,
        List<OrderItemResponse> items) {
}
