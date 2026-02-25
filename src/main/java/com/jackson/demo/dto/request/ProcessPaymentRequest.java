package com.jackson.demo.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ProcessPaymentRequest(
        @NotNull UUID paymentMethodId,
        String cvv) {
}
