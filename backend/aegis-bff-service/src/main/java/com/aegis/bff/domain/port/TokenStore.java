package com.aegis.bff.domain.port;

import java.util.Optional;

/**
 * Port for storing and retrieving JWT tokens associated with the current user session.
 */
public interface TokenStore {

    /**
     * Stores the access and refresh tokens for the current session.
     *
     * @param accessToken  the JWT access token
     * @param refreshToken the JWT refresh token
     */
    void storeTokens(String accessToken, String refreshToken);

    /**
     * @return the access token for the current session, if present
     */
    Optional<String> getAccessToken();

    /**
     * @return the refresh token for the current session, if present
     */
    Optional<String> getRefreshToken();

    /**
     * Clears all tokens from the current session.
     */
    void clear();
}
