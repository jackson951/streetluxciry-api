package com.jackson.demo.dto.request;
import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(
        @NotNull UUID productId,
        @NotNull @Min(1) Integer quantity) {
}
