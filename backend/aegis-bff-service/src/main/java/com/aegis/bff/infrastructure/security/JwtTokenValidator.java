package com.aegis.bff.infrastructure.security;

import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.infrastructure.config.BffProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates JWT tokens using the shared HMAC-SHA256 secret.
 *
 * <p>Replaces the unsafe Base64-only decoding that was previously used to extract claims.
 * The validator checks the cryptographic signature and ensures the token type is "access".</p>
 */
@Component
public class JwtTokenValidator implements TokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);
    private static final String EXPECTED_TOKEN_TYPE = "access";

    private final SecretKey secretKey;

    public JwtTokenValidator(BffProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(
                properties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Parses and validates the given JWT string.
     *
     * @param token the raw JWT
     * @return the validated claims
     * @throws JwtException if the token is invalid, expired, or has an unexpected type
     */
    public Claims validate(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String type = claims.get("type", String.class);
        if (!EXPECTED_TOKEN_TYPE.equals(type)) {
            throw new JwtException("Invalid token type: expected '" + EXPECTED_TOKEN_TYPE + "' but got '" + type + "'");
        }

        log.debug("JWT validated successfully for subject={}", claims.getSubject());
        return claims;
    }
}
