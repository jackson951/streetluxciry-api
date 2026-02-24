package com.jackson.demo.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @NotNull @Min(0) Integer stockQuantity,
        @NotNull Long categoryId,
        @NotNull @Size(min = 1, message = "At least one image URL is required") List<
                @NotBlank @Size(max = 700) String> imageUrls,
        Boolean active) {
}
