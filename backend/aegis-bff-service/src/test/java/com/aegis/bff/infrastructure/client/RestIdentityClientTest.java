package com.aegis.bff.infrastructure.client;

import com.aegis.bff.infrastructure.config.BffProperties;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RestIdentityClient")
class RestIdentityClientTest {

    private MockWebServer server;
    private RestIdentityClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        BffProperties properties = new BffProperties(
                new BffProperties.ServiceUrl(server.url("/").toString()),
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.Jwt("dummy-secret-that-is-at-least-256-bits-long-for-hs256-algorithm")
        );

        client = new RestIdentityClient(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @DisplayName("login - should send POST with email, password and correlation id")
    @org.junit.jupiter.api.Test
    void shouldLoginSuccessfully() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"accessToken":"at","refreshToken":"rt","expiresIn":900,"emailVerified":true}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.login("user@test.com", "pass", "corr-123");

        // Assert
        assertEquals("at", result.get("accessToken").asText());
        assertEquals("rt", result.get("refreshToken").asText());
        assertEquals(900, result.get("expiresIn").asInt());
        assertTrue(result.get("emailVerified").asBoolean());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/auth/login", request.getPath());
        assertEquals("corr-123", request.getHeader("X-Correlation-Id"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"email\":\"user@test.com\""));
        assertTrue(body.contains("\"password\":\"pass\""));
    }

    @DisplayName("refresh - should send POST with refresh token and correlation id")
    @org.junit.jupiter.api.Test
    void shouldRefreshSuccessfully() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"accessToken":"new-at","refreshToken":"new-rt","expiresIn":900}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.refresh("old-rt", "corr-456");

        // Assert
        assertEquals("new-at", result.get("accessToken").asText());
        assertEquals("new-rt", result.get("refreshToken").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/auth/refresh", request.getPath());
        assertEquals("corr-456", request.getHeader("X-Correlation-Id"));
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"refreshToken\":\"old-rt\""));
    }
}
