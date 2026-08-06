package com.aegis.bff.infrastructure.security;

import com.aegis.bff.domain.port.JwtSigningKey;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtTokenValidator")
class JwtTokenValidatorTest {

    private static final String TEST_SECRET = "aegis-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String WRONG_SECRET = "wrong-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm!!";
    private static final SecretKey WRONG_KEY = Keys.hmacShaKeyFor(WRONG_SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtTokenValidator validator;

    @BeforeEach
    void setUp() {
        JwtSigningKey signingKey = () -> SECRET_KEY;
        validator = new JwtTokenValidator(signingKey);
    }

    @Nested
    @DisplayName("When token is valid")
    class WhenTokenIsValid {

        @Test
        @DisplayName("Should return claims with correct subject and email")
        void shouldReturnClaims() {
            // Arrange
            Instant now = Instant.now();
            String token = Jwts.builder()
                    .subject("user-uuid")
                    .claim("email", "john@example.com")
                    .claim("type", "access")
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(SECRET_KEY)
                    .compact();

            // Act
            var claims = validator.validate(token);

            // Assert
            assertEquals("user-uuid", claims.getSubject());
            assertEquals("john@example.com", claims.get("email", String.class));
        }
    }

    @Nested
    @DisplayName("When token has invalid signature")
    class WhenTokenHasInvalidSignature {

        @Test
        @DisplayName("Should throw JwtException")
        void shouldThrowOnInvalidSignature() {
            // Arrange
            Instant now = Instant.now();
            String token = Jwts.builder()
                    .subject("user-uuid")
                    .claim("type", "access")
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(WRONG_KEY)
                    .compact();

            // Act & Assert
            assertThrows(JwtException.class, () -> validator.validate(token));
        }
    }

    @Nested
    @DisplayName("When token is expired")
    class WhenTokenIsExpired {

        @Test
        @DisplayName("Should throw JwtException")
        void shouldThrowOnExpiredToken() {
            // Arrange
            Instant past = Instant.now().minusSeconds(7200);
            String token = Jwts.builder()
                    .subject("user-uuid")
                    .claim("type", "access")
                    .issuedAt(Date.from(past.minusSeconds(3600)))
                    .expiration(Date.from(past))
                    .signWith(SECRET_KEY)
                    .compact();

            // Act & Assert
            assertThrows(JwtException.class, () -> validator.validate(token));
        }
    }

    @Nested
    @DisplayName("When token type is not 'access'")
    class WhenTokenTypeIsWrong {

        @Test
        @DisplayName("Should throw JwtException for refresh token type")
        void shouldThrowOnRefreshTokenType() {
            // Arrange
            Instant now = Instant.now();
            String token = Jwts.builder()
                    .subject("user-uuid")
                    .claim("type", "refresh")
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(SECRET_KEY)
                    .compact();

            // Act & Assert
            JwtException ex = assertThrows(JwtException.class, () -> validator.validate(token));
            assertTrue(ex.getMessage().contains("Invalid token type"));
        }

        @Test
        @DisplayName("Should throw JwtException when type claim is missing")
        void shouldThrowOnMissingType() {
            // Arrange
            Instant now = Instant.now();
            String token = Jwts.builder()
                    .subject("user-uuid")
                    .issuedAt(Date.from(now))
                    .expiration(Date.from(now.plusSeconds(3600)))
                    .signWith(SECRET_KEY)
                    .compact();

            // Act & Assert
            assertThrows(JwtException.class, () -> validator.validate(token));
        }
    }

    @Nested
    @DisplayName("When token is malformed")
    class WhenTokenIsMalformed {

        @Test
        @DisplayName("Should throw exception for garbage input")
        void shouldThrowOnMalformedToken() {
            assertThrows(Exception.class, () -> validator.validate("not-a-jwt"));
        }
    }
}
