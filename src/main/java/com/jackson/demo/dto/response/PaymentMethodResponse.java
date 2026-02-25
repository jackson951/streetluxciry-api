package com.jackson.demo.dto.response;
import java.util.UUID;

import com.jackson.demo.model.PaymentProvider;
import java.time.Instant;

public record PaymentMethodResponse(
        UUID id,
        PaymentProvider provider,
        String cardHolderName,
        String brand,
        String last4,
        Integer expiryMonth,
        Integer expiryYear,
        String billingAddress,
        boolean defaultMethod,
        boolean enabled,
        Instant createdAt) {
}
