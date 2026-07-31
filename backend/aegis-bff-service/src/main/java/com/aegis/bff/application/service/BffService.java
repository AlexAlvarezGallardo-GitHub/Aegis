package com.aegis.bff.application.service;

import com.aegis.bff.domain.port.IdentityClient;
import com.aegis.bff.domain.port.TokenStore;
import com.aegis.bff.domain.port.TokenValidator;
import com.fasterxml.jackson.databind.JsonNode;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Orchestrates BFF use cases: login, token refresh, logout, and current-user resolution.
 */
@Service
public class BffService {

    private final IdentityClient identityClient;
    private final TokenStore tokenStore;
    private final TokenValidator tokenValidator;

    public BffService(IdentityClient identityClient,
                      TokenStore tokenStore,
                      TokenValidator tokenValidator) {
        this.identityClient = identityClient;
        this.tokenStore = tokenStore;
        this.tokenValidator = tokenValidator;
    }

    /**
     * Authenticates the user against the Identity Service and stores the resulting tokens.
     *
     * @param email         the user's email
     * @param password      the user's password
     * @param correlationId the correlation id for tracing
     * @return a summary of the authentication result (token type, expiry, email-verified flag)
     */
    public Map<String, Object> login(String email, String password, String correlationId) {
        JsonNode response = identityClient.login(email, password, correlationId);

        String accessToken = response.get("accessToken").asText();
        String refreshToken = response.get("refreshToken").asText();
        tokenStore.storeTokens(accessToken, refreshToken);

        return Map.of(
                "tokenType", "Bearer",
                "expiresIn", response.get("expiresIn").asLong(),
                "emailVerified", response.get("emailVerified").asBoolean()
        );
    }

    /**
     * Refreshes the current access token using the stored refresh token.
     *
     * @param correlationId the correlation id for tracing
     * @return a summary of the refresh result
     */
    public Map<String, Object> refresh(String correlationId) {
        String refreshToken = tokenStore.getRefreshToken()
                .orElseThrow(() -> new IllegalStateException("No refresh token in session"));

        JsonNode response = identityClient.refresh(refreshToken, correlationId);

        String newAccessToken = response.get("accessToken").asText();
        String newRefreshToken = response.get("refreshToken").asText();
        tokenStore.storeTokens(newAccessToken, newRefreshToken);

        return Map.of(
                "tokenType", "Bearer",
                "expiresIn", response.get("expiresIn").asLong()
        );
    }

    /**
     * Clears all tokens from the current session.
     */
    public void logout() {
        tokenStore.clear();
    }

    /**
     * Resolves the current user by validating the stored access token.
     *
     * @return a map containing the user id and email extracted from the validated JWT
     */
    public Map<String, Object> getCurrentUser() {
        String accessToken = tokenStore.getAccessToken()
                .orElseThrow(() -> new IllegalStateException("Not authenticated"));

        Claims claims = tokenValidator.validate(accessToken);

        return Map.of(
                "userId", claims.getSubject(),
                "email", claims.get("email", String.class) != null
                        ? claims.get("email", String.class) : ""
        );
    }
}
