package com.jackson.demo.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stockQuantity,
        boolean active,
        List<String> imageUrls,
        CategoryResponse category) {
}
