package com.jackson.demo.dto.response;

import java.util.Set;

public record AuthUserResponse(
        Long id,
        String email,
        String fullName,
        Set<String> roles,
        Long customerId) {
}
