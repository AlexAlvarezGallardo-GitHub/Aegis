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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RestWalletClient")
class RestWalletClientTest {

    private MockWebServer server;
    private RestWalletClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        BffProperties properties = new BffProperties(
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.ServiceUrl(server.url("/").toString()),
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.Jwt("dummy-secret-that-is-at-least-256-bits-long-for-hs256-algorithm")
        );

        client = new RestWalletClient(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @DisplayName("createWallet - should POST with bearer token, user id and currency")
    @org.junit.jupiter.api.Test
    void shouldCreateWallet() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"walletId":"w1","currency":"USD","balance":0,"status":"ACTIVE"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.createWallet("access-token", "user-1", "USD", "corr-1");

        // Assert
        assertEquals("w1", result.get("walletId").asText());
        assertEquals("USD", result.get("currency").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/wallets", request.getPath());
        assertEquals("Bearer access-token", request.getHeader("Authorization"));
        assertEquals("user-1", request.getHeader("X-User-Id"));
        assertEquals("corr-1", request.getHeader("X-Correlation-Id"));
    }

    @DisplayName("listWallets - should GET with bearer token and user id")
    @org.junit.jupiter.api.Test
    void shouldListWallets() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        [{"walletId":"w1"},{"walletId":"w2"}]
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.listWallets("access-token", "user-1");

        // Assert
        assertTrue(result.isArray());
        assertEquals(2, result.size());

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/wallets", request.getPath());
        assertEquals("Bearer access-token", request.getHeader("Authorization"));
        assertEquals("user-1", request.getHeader("X-User-Id"));
    }

    @DisplayName("getWallet - should GET specific wallet by id")
    @org.junit.jupiter.api.Test
    void shouldGetWallet() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"walletId":"w1","currency":"EUR","balance":100}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.getWallet("access-token", "user-1", "w1");

        // Assert
        assertEquals("w1", result.get("walletId").asText());
        assertEquals("EUR", result.get("currency").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/wallets/w1", request.getPath());
    }

    @DisplayName("adjustBalance - should PATCH with amount and optional description")
    @org.junit.jupiter.api.Test
    void shouldAdjustBalanceWithDescription() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"walletId":"w1","balance":150}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.adjustBalance("access-token", "user-1", "w1",
                new BigDecimal("50.00"), "bonus", "corr-2");

        // Assert
        assertEquals(150, result.get("balance").asInt());

        RecordedRequest request = server.takeRequest();
        assertEquals("PATCH", request.getMethod());
        assertEquals("/api/v1/wallets/w1/balance", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"amount\":50.00"));
        assertTrue(body.contains("\"description\":\"bonus\""));
    }

    @DisplayName("adjustBalance - should omit description when null")
    @org.junit.jupiter.api.Test
    void shouldAdjustBalanceWithoutDescription() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"walletId":"w1","balance":50}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.adjustBalance("access-token", "user-1", "w1",
                new BigDecimal("10.00"), null, "corr-3");

        // Assert
        assertEquals(50, result.get("balance").asInt());

        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"amount\":10.00"));
        assertFalse(body.contains("description"));
    }

    @DisplayName("depositFunds - should POST with amount, currency, source and optional reference")
    @org.junit.jupiter.api.Test
    void shouldDepositFundsWithReference() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"walletId":"w1","depositId":"d1","status":"COMPLETED"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.depositFunds("access-token", "user-1", "w1",
                new BigDecimal("100.00"), "EUR", "BANK_TRANSFER", "ref-123", "corr-4");

        // Assert
        assertEquals("COMPLETED", result.get("status").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/wallets/w1/deposits", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"amount\":100.00"));
        assertTrue(body.contains("\"currency\":\"EUR\""));
        assertTrue(body.contains("\"source\":\"BANK_TRANSFER\""));
        assertTrue(body.contains("\"reference\":\"ref-123\""));
    }

    @DisplayName("depositFunds - should omit reference when null")
    @org.junit.jupiter.api.Test
    void shouldDepositFundsWithoutReference() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"walletId":"w1","depositId":"d2","status":"COMPLETED"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.depositFunds("access-token", "user-1", "w1",
                new BigDecimal("25.00"), "EUR", "CARD", null, "corr-5");

        // Assert
        assertEquals("COMPLETED", result.get("status").asText());

        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"currency\":\"EUR\""));
        assertTrue(body.contains("\"source\":\"CARD\""));
        assertFalse(body.contains("reference"));
    }

    @DisplayName("updateStatus - should PATCH with new status")
    @org.junit.jupiter.api.Test
    void shouldUpdateStatus() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"walletId":"w1","status":"FROZEN"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.updateStatus("access-token", "user-1", "w1", "FROZEN");

        // Assert
        assertEquals("FROZEN", result.get("status").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("PATCH", request.getMethod());
        assertEquals("/api/v1/wallets/w1/status", request.getPath());
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"status\":\"FROZEN\""));
    }
}
