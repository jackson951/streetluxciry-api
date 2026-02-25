package com.jackson.demo.dto.response;
import java.util.UUID;

import java.util.Set;

public record AuthUserResponse(
        UUID id,
        String email,
        String fullName,
        Set<String> roles,
        UUID customerId) {
}
