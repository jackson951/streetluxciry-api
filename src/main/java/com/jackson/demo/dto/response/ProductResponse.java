package com.jackson.demo.dto.response;
import java.util.UUID;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        boolean active,
        List<String> imageUrls,
        CategoryResponse category) {
}
