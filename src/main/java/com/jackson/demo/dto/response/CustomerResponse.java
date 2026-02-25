package com.jackson.demo.dto.response;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        String address) {
}
