package com.aegis.bff;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BffServiceTest {

    private MockWebServer mockIdentity;
    private ObjectMapper objectMapper;
    private StubSessionJwtStore sessionJwtStore;
    private BffService service;

    @BeforeEach
    void setUp() throws Exception {
        mockIdentity = new MockWebServer();
        mockIdentity.start();
        objectMapper = new ObjectMapper();
        sessionJwtStore = new StubSessionJwtStore();

        RestClient restClient = RestClient.builder()
                .baseUrl(mockIdentity.url("/").toString())
                .build();

        service = new BffService(restClient, objectMapper, sessionJwtStore);
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
        assertEquals("access-123", sessionJwtStore.storedAccessToken);
        assertEquals("refresh-456", sessionJwtStore.storedRefreshToken);
    }

    @Test
    void shouldRefreshToken() {
        sessionJwtStore.storedRefreshToken = "old-refresh";

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
        assertEquals("new-access", sessionJwtStore.storedAccessToken);
        assertEquals("new-refresh", sessionJwtStore.storedRefreshToken);
    }

    @Test
    void shouldThrowOnRefreshWhenNoToken() {
        var ex = assertThrows(RuntimeException.class, () -> service.refresh("corr-3"));
        assertTrue(ex.getMessage().contains("No refresh token in session"));
    }

    @Test
    void shouldLogout() {
        sessionJwtStore.storedAccessToken = "token";
        service.logout();
        assertTrue(sessionJwtStore.cleared);
    }

    @Test
    void shouldGetCurrentUserFromToken() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "sub", "user-uuid",
                "email", "john@example.com"
        ));
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
        String jwt = "header." + encoded + ".signature";
        sessionJwtStore.storedAccessToken = jwt;

        Map<String, Object> user = service.getCurrentUser();

        assertEquals(Map.of("userId", "user-uuid", "email", "john@example.com"), user);
    }

    @Test
    void shouldThrowOnGetCurrentUserWhenNoToken() {
        assertThrows(RuntimeException.class, () -> service.getCurrentUser());
    }

    static class StubSessionJwtStore extends SessionJwtStore {
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
}
