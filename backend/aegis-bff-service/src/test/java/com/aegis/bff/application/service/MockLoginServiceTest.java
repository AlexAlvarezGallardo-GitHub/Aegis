package com.aegis.bff.application.service;

import com.aegis.bff.domain.port.TokenStore;
import com.aegis.bff.infrastructure.config.BffProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MockLoginService")
class MockLoginServiceTest {

    private static final String TEST_SECRET = "aegis-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    private InMemoryTokenStore tokenStore;
    private MockLoginService service;

    @BeforeEach
    void setUp() {
        tokenStore = new InMemoryTokenStore();
        BffProperties props = new BffProperties(
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.Jwt(TEST_SECRET)
        );
        service = new MockLoginService(tokenStore, props);
    }

    @Test
    @DisplayName("Should generate valid access and refresh tokens and store them")
    void shouldGenerateAndStoreTokens() {
        // Act
        Map<String, Object> result = service.mockLogin();

        // Assert
        assertEquals("Bearer", result.get("tokenType"));
        assertEquals(86400L, result.get("expiresIn"));
        assertEquals(true, result.get("emailVerified"));
        assertEquals(true, result.get("mock"));

        assertNotNull(tokenStore.accessToken);
        assertNotNull(tokenStore.refreshToken);
    }

    @Test
    @DisplayName("Generated access token should be valid and contain correct claims")
    void generatedAccessTokenShouldBeValid() {
        // Act
        service.mockLogin();

        // Assert - validate the access token
        var claims = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(tokenStore.accessToken)
                .getPayload();

        assertEquals(MockLoginService.getMockUserId().toString(), claims.getSubject());
        assertEquals("mock@aegis.dev", claims.get("email", String.class));
        assertEquals("access", claims.get("type", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("Generated refresh token should have type 'refresh'")
    void generatedRefreshTokenShouldHaveRefreshType() {
        // Act
        service.mockLogin();

        // Assert
        var claims = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(tokenStore.refreshToken)
                .getPayload();

        assertEquals("refresh", claims.get("type", String.class));
        assertEquals(MockLoginService.getMockUserId().toString(), claims.getSubject());
    }

    @Test
    @DisplayName("getMockUserId should return the well-known mock UUID")
    void shouldReturnMockUserId() {
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"),
                MockLoginService.getMockUserId());
    }

    static class InMemoryTokenStore implements TokenStore {
        String accessToken;
        String refreshToken;

        @Override
        public void storeTokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        @Override
        public Optional<String> getAccessToken() {
            return Optional.ofNullable(accessToken);
        }

        @Override
        public Optional<String> getRefreshToken() {
            return Optional.ofNullable(refreshToken);
        }

        @Override
        public void clear() {
            this.accessToken = null;
            this.refreshToken = null;
        }
    }
}
