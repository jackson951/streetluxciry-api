package com.jackson.demo.dto.response;
import java.util.UUID;

import java.time.Instant;
import java.util.Set;

public record AdminUserResponse(
        UUID id,
        String email,
        String fullName,
        Set<String> roles,
        boolean enabled,
        UUID customerId,
        Instant createdAt) {
}
