package com.jackson.demo.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCheckoutSessionRequest(
        UUID customerId,
        UUID preferredPaymentMethodId,
        @Size(max = 400) String shippingAddress,
        @NotNull Boolean isDelivery) {
}
