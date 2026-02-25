package com.jackson.demo.dto.response;

import com.jackson.demo.model.OrderStatus;

public record OrderTrackingStageResponse(
        int step,
        OrderStatus status,
        String label,
        boolean completed,
        boolean current) {
}
