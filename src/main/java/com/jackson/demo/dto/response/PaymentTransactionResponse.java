package com.jackson.demo.dto.response;
import java.util.UUID;

import com.jackson.demo.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentTransactionResponse(
        UUID id,
        UUID orderId,
        UUID customerId,
        UUID paymentMethodId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String gatewayResponseCode,
        String gatewayMessage,
        Instant processedAt) {
}
