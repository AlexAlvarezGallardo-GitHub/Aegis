package com.aegis.bff.domain.port;

import io.jsonwebtoken.Claims;

/**
 * Port for validating JWT tokens and extracting their claims.
 */
public interface TokenValidator {

    /**
     * Validates the given JWT token and returns its claims.
     *
     * @param token the raw JWT string
     * @return the validated claims
     * @throws io.jsonwebtoken.JwtException if the token is invalid, expired, or has an unexpected type
     */
    Claims validate(String token);
}
