package com.jackson.demo.dto.response;

import java.time.Instant;

public record OtpResponse(
        String email,
        String type,
        Instant expiresAt,
        boolean used
) {}