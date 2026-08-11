package com.aegis.bff.infrastructure.client;

import com.aegis.bff.infrastructure.config.BffProperties;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RestPaymentClient")
class RestPaymentClientTest {

    private MockWebServer server;
    private RestPaymentClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        BffProperties properties = new BffProperties(
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.ServiceUrl("http://localhost"),
                new BffProperties.ServiceUrl(server.url("/").toString()),
                new BffProperties.Jwt("dummy-secret-that-is-at-least-256-bits-long-for-hs256-algorithm")
        );

        client = new RestPaymentClient(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @DisplayName("transferFunds - should POST with bearer token, user id, body fields and correlation id")
    @org.junit.jupiter.api.Test
    void shouldTransferFunds() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"transferId":"t1","status":"PENDING","sourceWalletId":"w1","destWalletId":"w2"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.transferFunds("access-token", "user-1",
                "w1", "w2", new BigDecimal("25.50"), "USD", "bonus", "ref-1", "corr-1");

        // Assert
        assertEquals("t1", result.get("transferId").asText());
        assertEquals("PENDING", result.get("status").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/transfers", request.getPath());
        assertEquals("Bearer access-token", request.getHeader("Authorization"));
        assertEquals("user-1", request.getHeader("X-User-Id"));
        assertEquals("corr-1", request.getHeader("X-Correlation-Id"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"sourceWalletId\":\"w1\""));
        assertTrue(body.contains("\"destWalletId\":\"w2\""));
        assertTrue(body.contains("\"userId\":\"user-1\""));
        assertTrue(body.contains("\"amount\":25.50"));
        assertTrue(body.contains("\"currency\":\"USD\""));
        assertTrue(body.contains("\"description\":\"bonus\""));
        assertTrue(body.contains("\"reference\":\"ref-1\""));
    }

    @DisplayName("transferFunds - should omit description when null")
    @org.junit.jupiter.api.Test
    void shouldTransferFundsWithoutDescription() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"transferId":"t2","status":"PENDING"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.transferFunds("access-token", "user-1",
                "w1", "w2", new BigDecimal("10.00"), "EUR", null, "ref-2", "corr-2");

        // Assert
        assertEquals("t2", result.get("transferId").asText());

        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"sourceWalletId\":\"w1\""));
        assertFalse(body.contains("description"));
    }

    @DisplayName("getTransfer - should GET specific transfer by id with bearer/user/correlation headers")
    @org.junit.jupiter.api.Test
    void shouldGetTransfer() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"transferId":"t1","status":"COMPLETED","amount":25.50}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.getTransfer("access-token", "user-1", "t1", "corr-3");

        // Assert
        assertEquals("t1", result.get("transferId").asText());
        assertEquals("COMPLETED", result.get("status").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/transfers/t1", request.getPath());
        assertEquals("Bearer access-token", request.getHeader("Authorization"));
        assertEquals("user-1", request.getHeader("X-User-Id"));
        assertEquals("corr-3", request.getHeader("X-Correlation-Id"));
    }

    @DisplayName("getTransfer - should propagate 4xx errors")
    @org.junit.jupiter.api.Test
    void shouldPropagate4xxOnGetTransfer() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("""
                        {"error":"TRANSFER_NOT_FOUND"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act & Assert
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> client.getTransfer("access-token", "user-1", "missing", "corr-4"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @DisplayName("transferFunds - should propagate 4xx errors")
    @org.junit.jupiter.api.Test
    void shouldPropagate4xxOnTransferFunds() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setResponseCode(422)
                .setBody("""
                        {"error":"INSUFFICIENT_FUNDS"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act & Assert
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> client.transferFunds("access-token", "user-1",
                        "w1", "w2", new BigDecimal("9999.99"), "USD", null, "ref-x", "corr-5"));
        assertEquals(422, ex.getStatusCode().value());
    }
}
