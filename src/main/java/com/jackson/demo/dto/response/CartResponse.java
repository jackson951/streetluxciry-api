package com.jackson.demo.dto.response;
import java.util.UUID;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        UUID id,
        UUID customerId,
        List<CartItemResponse> items,
        BigDecimal totalAmount) {
}
