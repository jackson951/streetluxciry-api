package com.jackson.demo.dto.request;

import jakarta.validation.constraints.NotNull;

public record ProcessPaymentRequest(
        @NotNull Long paymentMethodId,
        String cvv) {
}
