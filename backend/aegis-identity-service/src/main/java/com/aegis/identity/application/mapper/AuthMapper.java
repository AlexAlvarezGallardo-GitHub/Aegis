package com.aegis.identity.application.mapper;

import com.aegis.identity.application.dto.AuthenticationResponse;
import com.aegis.identity.domain.model.TokenPair;

public final class AuthMapper {

    private static final long ACCESS_TOKEN_EXPIRY_SECONDS = 900;

    private AuthMapper() {
    }

    public static AuthenticationResponse toResponse(TokenPair tokenPair, boolean emailVerified) {
        return AuthenticationResponse.of(tokenPair, emailVerified, ACCESS_TOKEN_EXPIRY_SECONDS);
    }
}
