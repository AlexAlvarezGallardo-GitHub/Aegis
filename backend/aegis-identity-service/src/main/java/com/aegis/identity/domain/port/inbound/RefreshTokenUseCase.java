package com.aegis.identity.domain.port.inbound;

import com.aegis.identity.domain.model.TokenPair;

/**
 * Inbound port for refreshing an access token using a valid refresh token.
 */
public interface RefreshTokenUseCase {

    /**
     * Refreshes the access token described by the given command.
     *
     * @param command the refresh request containing the refresh token and correlation id
     * @return the refresh result with the new token pair
     */
    Result refresh(Command command);

    /**
     * Command data for refreshing a token pair.
     *
     * @param refreshToken  the valid refresh token
     * @param correlationId the client correlation id for tracing the request
     */
    record Command(String refreshToken, String correlationId) {
    }

    /**
     * Result data returned after a successful token refresh.
     *
     * @param tokenPair the new access and refresh tokens
     */
    record Result(TokenPair tokenPair) {
    }
}
