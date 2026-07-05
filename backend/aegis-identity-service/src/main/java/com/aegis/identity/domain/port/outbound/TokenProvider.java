package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.model.TokenPair;
import com.aegis.identity.domain.model.UserId;

/**
 * Outbound port for generating and validating JWT access and refresh tokens.
 */
public interface TokenProvider {

    /**
     * Generates a new access and refresh token pair for the given user.
     *
     * @param userId the user identifier
     * @param email  the user email address
     * @return a token pair containing access and refresh tokens
     */
    TokenPair generateTokenPair(UserId userId, String email);

    /**
     * Generates a new access token for the given user.
     *
     * @param userId the user identifier
     * @param email  the user email address
     * @return the encoded access token
     */
    String generateAccessToken(UserId userId, String email);

    /**
     * Generates a new refresh token for the given user.
     *
     * @param userId the user identifier
     * @param email  the user email address
     * @return the encoded refresh token
     */
    String generateRefreshToken(UserId userId, String email);

    /**
     * Validates the given access token and returns the associated user identifier.
     *
     * @param token the encoded access token
     * @return the user identifier extracted from the token
     */
    UserId validateAccessToken(String token);

    /**
     * Validates the given refresh token and returns the associated user identifier.
     *
     * @param token the encoded refresh token
     * @return the user identifier extracted from the token
     */
    UserId validateRefreshToken(String token);
}
