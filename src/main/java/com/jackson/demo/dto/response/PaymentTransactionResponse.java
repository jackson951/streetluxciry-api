package com.jackson.demo.dto.response;

import com.jackson.demo.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentTransactionResponse(
        Long id,
        Long orderId,
        Long customerId,
        Long paymentMethodId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String gatewayResponseCode,
        String gatewayMessage,
        Instant processedAt) {
}
