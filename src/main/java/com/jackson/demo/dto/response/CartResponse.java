package com.jackson.demo.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        Long customerId,
        List<CartItemResponse> items,
        BigDecimal totalAmount) {
}
