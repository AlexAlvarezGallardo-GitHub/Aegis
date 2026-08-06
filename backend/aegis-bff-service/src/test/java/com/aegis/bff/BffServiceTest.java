package com.aegis.bff;

import com.aegis.bff.application.service.BffService;
import com.aegis.bff.domain.port.IdentityClient;
import com.aegis.bff.domain.port.TokenStore;
import com.aegis.bff.domain.port.TokenValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BffServiceTest {

    private static final String TEST_SECRET = "aegis-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    private MockWebServer mockIdentity;
    private TokenStore tokenStore;
    private TokenValidator tokenValidator;
    private BffService service;

    @BeforeEach
    void setUp() throws Exception {
        mockIdentity = new MockWebServer();
        mockIdentity.start();
        tokenStore = new InMemoryTokenStore();
        // Use a real TokenValidator that validates JWTs signed with our test secret
        tokenValidator = new com.aegis.bff.infrastructure.security.JwtTokenValidator(
                () -> SECRET_KEY
        );

        // A simple IdentityClient backed by MockWebServer
        IdentityClient identityClient = new MockIdentityClient(
                mockIdentity.url("/").toString(), new ObjectMapper());

        service = new BffService(identityClient, tokenStore, tokenValidator);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockIdentity.shutdown();
    }

    @Test
    void shouldLoginAndStoreTokens() {
        mockIdentity.enqueue(new MockResponse()
                .setBody("""
                        {
                          "accessToken": "access-123",
                          "refreshToken": "refresh-456",
                          "tokenType": "Bearer",
                          "expiresIn": 900,
                          "emailVerified": true
                        }""")
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> result = service.login("user@test.com", "pass", "corr-1");

        assertEquals(Map.of("tokenType", "Bearer", "expiresIn", 900L, "emailVerified", true), result);

        InMemoryTokenStore memStore = (InMemoryTokenStore) tokenStore;
        assertEquals("access-123", memStore.storedAccessToken);
        assertEquals("refresh-456", memStore.storedRefreshToken);
    }

    @Test
    void shouldRefreshToken() {
        InMemoryTokenStore memStore = (InMemoryTokenStore) tokenStore;
        memStore.storedRefreshToken = "old-refresh";

        mockIdentity.enqueue(new MockResponse()
                .setBody("""
                        {
                          "accessToken": "new-access",
                          "refreshToken": "new-refresh",
                          "tokenType": "Bearer",
                          "expiresIn": 900
                        }""")
                .addHeader("Content-Type", "application/json"));

        Map<String, Object> result = service.refresh("corr-2");

        assertEquals(Map.of("tokenType", "Bearer", "expiresIn", 900L), result);
        assertEquals("new-access", memStore.storedAccessToken);
        assertEquals("new-refresh", memStore.storedRefreshToken);
    }

    @Test
    void shouldThrowOnRefreshWhenNoToken() {
        var ex = assertThrows(IllegalStateException.class, () -> service.refresh("corr-3"));
        assertTrue(ex.getMessage().contains("No refresh token in session"));
    }

    @Test
    void shouldLogout() {
        InMemoryTokenStore memStore = (InMemoryTokenStore) tokenStore;
        memStore.storedAccessToken = "token";
        service.logout();
        assertTrue(memStore.cleared);
    }

    @Test
    void shouldGetCurrentUserFromValidatedToken() {
        InMemoryTokenStore memStore = (InMemoryTokenStore) tokenStore;

        // Build a real signed JWT with the test secret
        Instant now = Instant.now();
        String jwt = Jwts.builder()
                .subject("user-uuid")
                .claim("email", "john@example.com")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(SECRET_KEY)
                .compact();
        memStore.storedAccessToken = jwt;

        Map<String, Object> user = service.getCurrentUser();

        assertEquals(Map.of("userId", "user-uuid", "email", "john@example.com"), user);
    }

    @Test
    void shouldThrowOnGetCurrentUserWhenNoToken() {
        assertThrows(IllegalStateException.class, () -> service.getCurrentUser());
    }

    @Test
    void shouldReturnEmptyEmailWhenClaimMissing() {
        InMemoryTokenStore memStore = (InMemoryTokenStore) tokenStore;

        // Build a JWT without the email claim
        Instant now = Instant.now();
        String jwt = Jwts.builder()
                .subject("user-uuid")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(SECRET_KEY)
                .compact();
        memStore.storedAccessToken = jwt;

        Map<String, Object> user = service.getCurrentUser();

        assertEquals("user-uuid", user.get("userId"));
        assertEquals("", user.get("email"));
    }

    // ----- Test helpers -----

    /**
     * Simple in-memory TokenStore for tests.
     */
    static class InMemoryTokenStore implements TokenStore {
        String storedAccessToken;
        String storedRefreshToken;
        boolean cleared = false;

        @Override
        public void storeTokens(String accessToken, String refreshToken) {
            this.storedAccessToken = accessToken;
            this.storedRefreshToken = refreshToken;
        }

        @Override
        public Optional<String> getAccessToken() {
            return Optional.ofNullable(storedAccessToken);
        }

        @Override
        public Optional<String> getRefreshToken() {
            return Optional.ofNullable(storedRefreshToken);
        }

        @Override
        public void clear() {
            this.cleared = true;
            this.storedAccessToken = null;
            this.storedRefreshToken = null;
        }
    }

    /**
     * IdentityClient backed by a MockWebServer URL.
     */
    static class MockIdentityClient implements IdentityClient {
        private final org.springframework.web.client.RestClient restClient;
        private final ObjectMapper objectMapper;

        MockIdentityClient(String baseUrl, ObjectMapper objectMapper) {
            this.restClient = org.springframework.web.client.RestClient.builder()
                    .baseUrl(baseUrl).build();
            this.objectMapper = objectMapper;
        }

        @Override
        public JsonNode login(String email, String password, String correlationId) {
            return restClient.post()
                    .uri("/api/v1/auth/login")
                    .header("X-Correlation-Id", correlationId)
                    .body(Map.of("email", email, "password", password))
                    .retrieve()
                    .body(JsonNode.class);
        }

        @Override
        public JsonNode refresh(String refreshToken, String correlationId) {
            return restClient.post()
                    .uri("/api/v1/auth/refresh")
                    .header("X-Correlation-Id", correlationId)
                    .body(Map.of("refreshToken", refreshToken))
                    .retrieve()
                    .body(JsonNode.class);
        }
    }
}
