package com.jackson.demo.dto.response;

public record AuthTokenResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresInSeconds,
        String refreshToken,
        AuthUserResponse user) {
}
