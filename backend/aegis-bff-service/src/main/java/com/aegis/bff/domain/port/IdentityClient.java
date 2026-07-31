package com.aegis.bff.domain.port;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Outbound port for communicating with the Identity Service.
 */
public interface IdentityClient {

    /**
     * Authenticates a user with email and password.
     *
     * @param email         the user's email
     * @param password      the user's password
     * @param correlationId the correlation id for tracing
     * @return the authentication response containing tokens
     */
    JsonNode login(String email, String password, String correlationId);

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshToken  the refresh token
     * @param correlationId the correlation id for tracing
     * @return the refresh response containing new tokens
     */
    JsonNode refresh(String refreshToken, String correlationId);
}
