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

    /**
     * Creates an authentication response with only an access token (no refresh token).
     *
     * @param accessToken   the JWT access token
     * @param emailVerified whether the user's email has been verified
     * @param expiresIn     the access token expiry in seconds
     * @return the authentication response
     */
    public static AuthenticationResponse ofAccessTokenOnly(String accessToken, boolean emailVerified, long expiresIn) {
        return new AuthenticationResponse(accessToken, null, "Bearer", expiresIn, emailVerified);
    }
}
