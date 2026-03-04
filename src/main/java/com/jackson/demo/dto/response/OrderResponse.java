package com.jackson.demo.dto.response;
import java.util.UUID;

import com.jackson.demo.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        UUID id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal deliveryFee,
        Boolean isDelivery,
        String shippingAddress,
        Instant createdAt,
        UUID customerId,
        List<OrderItemResponse> items) {
}
