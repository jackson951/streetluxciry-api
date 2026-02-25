package com.jackson.demo.dto.response;
import java.util.UUID;

import com.jackson.demo.model.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderTrackingResponse(
        UUID orderId,
        String orderNumber,
        OrderStatus currentStatus,
        Instant createdAt,
        List<OrderTrackingStageResponse> stages) {
}
