package com.jackson.demo.dto.response;

public record CustomerResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String address) {
}
