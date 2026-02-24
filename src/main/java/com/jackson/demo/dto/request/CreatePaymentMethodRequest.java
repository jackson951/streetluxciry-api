package com.jackson.demo.dto.request;

import com.jackson.demo.model.PaymentProvider;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePaymentMethodRequest(
        PaymentProvider provider,
        @NotBlank @Size(max = 140) String cardHolderName,
        @NotBlank @Pattern(regexp = "^[0-9 ]{12,23}$", message = "Card number must contain only digits and spaces")
                String cardNumber,
        @Size(max = 25) String brand,
        @NotNull @Min(1) @Max(12) Integer expiryMonth,
        @NotNull @Min(2024) @Max(2100) Integer expiryYear,
        @Size(max = 400) String billingAddress,
        Boolean defaultMethod) {
}
