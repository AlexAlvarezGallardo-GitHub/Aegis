package com.aegis.identity.application.mapper;

import com.aegis.identity.application.dto.AuthenticationResponse;
import com.aegis.identity.domain.model.TokenPair;

/**
 * Mapper for authentication-related DTOs.
 */
public final class AuthMapper {

    private AuthMapper() {
    }

    /**
     * Maps a token pair and email verification status to an authentication response.
     *
     * @param tokenPair        the access and refresh tokens
     * @param emailVerified    whether the user's email has been verified
     * @param expiresInSeconds the access token expiry in seconds (from configuration)
     * @return the authentication response DTO
     */
    public static AuthenticationResponse toResponse(TokenPair tokenPair, boolean emailVerified,
                                                     long expiresInSeconds) {
        return AuthenticationResponse.of(tokenPair, emailVerified, expiresInSeconds);
    }
}
