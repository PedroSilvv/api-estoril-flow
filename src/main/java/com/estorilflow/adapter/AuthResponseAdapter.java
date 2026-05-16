package com.estorilflow.adapter;

import com.estorilflow.config.security.AuthenticatedUserDetails;
import com.estorilflow.config.security.JwtTokenService;
import com.estorilflow.dto.AuthResponse;
import com.estorilflow.dto.AuthenticatedUserResponse;

public final class AuthResponseAdapter {

    private AuthResponseAdapter() {
    }

    public static AuthResponse toResponse(
            JwtTokenService.TokenDetails tokenDetails,
            AuthenticatedUserDetails user
    ) {
        return new AuthResponse(
                tokenDetails.token(),
                "Bearer",
                tokenDetails.expiresIn(),
                tokenDetails.issuedAt(),
                tokenDetails.expiresAt(),
                toUserResponse(user)
        );
    }

    public static AuthenticatedUserResponse toUserResponse(AuthenticatedUserDetails user) {
        return new AuthenticatedUserResponse(
                user.id(),
                user.name(),
                user.getUsername(),
                user.email(),
                user.role()
        );
    }
}
