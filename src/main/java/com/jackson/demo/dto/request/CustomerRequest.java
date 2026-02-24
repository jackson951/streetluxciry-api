package com.jackson.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank @Size(max = 140) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @Size(max = 30) String phone,
        @Size(max = 400) String address) {
}
