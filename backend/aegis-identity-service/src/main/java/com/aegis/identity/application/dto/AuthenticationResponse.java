package com.aegis.identity.application.dto;

import com.aegis.identity.domain.model.TokenPair;

public record AuthenticationResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean emailVerified
) {
    public static AuthenticationResponse of(TokenPair tokenPair, boolean emailVerified, long expiresIn) {
        return new AuthenticationResponse(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                "Bearer",
                expiresIn,
                emailVerified
        );
    }
}
