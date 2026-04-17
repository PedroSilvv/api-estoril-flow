package com.estorilflow.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant issuedAt,
        Instant expiresAt,
        AuthenticatedUserResponse user
) {
}
