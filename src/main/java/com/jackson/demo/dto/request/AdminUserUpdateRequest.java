package com.jackson.demo.dto.request;

import com.jackson.demo.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record AdminUserUpdateRequest(
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Size(max = 140) String fullName,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotEmpty Set<UserRole> roles,
        boolean enabled,
        @Size(max = 30) String phone,
        @Size(max = 400) String address) {
}
