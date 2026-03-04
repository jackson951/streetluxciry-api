package com.jackson.demo.dto.response;
import java.util.UUID;

import com.jackson.demo.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderTrackingResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus currentStatus,
        BigDecimal deliveryFee,
        Boolean isDelivery,
        String shippingAddress,
        Instant createdAt,
        List<OrderTrackingStageResponse> stages) {
}
