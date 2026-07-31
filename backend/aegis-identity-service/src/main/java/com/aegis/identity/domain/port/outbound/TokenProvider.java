package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.model.UserId;

/**
 * Outbound port for generating and validating JWT access and refresh tokens.
 */
public interface TokenProvider {

    /**
     * Generates a new access token for the given user.
     *
     * @param userId the user identifier
     * @param email  the user email address
     * @return the encoded access token
     */
    String generateAccessToken(UserId userId, String email);

    /**
     * Validates the given access token and returns the associated user identifier.
     *
     * @param token the encoded access token
     * @return the user identifier extracted from the token
     */
    UserId validateAccessToken(String token);

    /**
     * Returns the configured access token expiry in seconds.
     *
     * @return the access token expiry duration in seconds
     */
    long getAccessTokenExpirySeconds();
}
