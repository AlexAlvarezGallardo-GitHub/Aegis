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

@DisplayName("RestPaymentExecutionClient")
class RestPaymentExecutionClientTest {

    private MockWebServer server;
    private RestPaymentExecutionClient client;

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

        client = new RestPaymentExecutionClient(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @DisplayName("executePayment - should POST with bearer token, user id header, payee body and correlation id")
    @org.junit.jupiter.api.Test
    void shouldExecutePayment() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"paymentId":"p1","status":"COMPLETED","walletId":"w1","amount":25.50}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.executePayment("access-token", "user-1",
                "w1", new BigDecimal("25.50"), "USD",
                "Acme Corp", "acme-001", "MERCHANT",
                "purchase", "PAY-1", "corr-1");

        // Assert
        assertEquals("p1", result.get("paymentId").asText());
        assertEquals("COMPLETED", result.get("status").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/payments", request.getPath());
        assertEquals("Bearer access-token", request.getHeader("Authorization"));
        assertEquals("user-1", request.getHeader("X-User-Id"));
        assertEquals("corr-1", request.getHeader("X-Correlation-Id"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"walletId\":\"w1\""));
        assertTrue(body.contains("\"amount\":25.50"));
        assertTrue(body.contains("\"currency\":\"USD\""));
        assertTrue(body.contains("\"name\":\"Acme Corp\""));
        assertTrue(body.contains("\"id\":\"acme-001\""));
        assertTrue(body.contains("\"type\":\"MERCHANT\""));
        assertTrue(body.contains("\"description\":\"purchase\""));
        assertTrue(body.contains("\"reference\":\"PAY-1\""));
        // userId must NOT be in the body — it is sent as a header
        assertFalse(body.contains("\"userId\""));
    }

    @DisplayName("executePayment - should omit description when null")
    @org.junit.jupiter.api.Test
    void shouldExecutePaymentWithoutDescription() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"paymentId":"p2","status":"COMPLETED"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.executePayment("access-token", "user-1",
                "w1", new BigDecimal("10.00"), "EUR",
                "Store", "store-002", "INDIVIDUAL",
                null, "PAY-2", "corr-2");

        // Assert
        assertEquals("p2", result.get("paymentId").asText());

        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"walletId\":\"w1\""));
        assertFalse(body.contains("description"));
    }

    @DisplayName("getPayment - should GET specific payment by id with bearer/user/correlation headers")
    @org.junit.jupiter.api.Test
    void shouldGetPayment() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"paymentId":"p1","status":"COMPLETED","amount":25.50}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.getPayment("access-token", "user-1", "p1", "corr-3");

        // Assert
        assertEquals("p1", result.get("paymentId").asText());
        assertEquals("COMPLETED", result.get("status").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/api/v1/payments/p1", request.getPath());
        assertEquals("Bearer access-token", request.getHeader("Authorization"));
        assertEquals("user-1", request.getHeader("X-User-Id"));
        assertEquals("corr-3", request.getHeader("X-Correlation-Id"));
    }

    @DisplayName("getPayment - should propagate 4xx errors")
    @org.junit.jupiter.api.Test
    void shouldPropagate4xxOnGetPayment() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("""
                        {"error":"PAYMENT_NOT_FOUND"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act & Assert
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> client.getPayment("access-token", "user-1", "missing", "corr-4"));
        assertEquals(404, ex.getStatusCode().value());
    }

    @DisplayName("executePayment - should propagate 4xx errors")
    @org.junit.jupiter.api.Test
    void shouldPropagate4xxOnExecutePayment() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setResponseCode(422)
                .setBody("""
                        {"error":"INSUFFICIENT_FUNDS"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act & Assert
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> client.executePayment("access-token", "user-1",
                        "w1", new BigDecimal("9999.99"), "USD",
                        "Payee", "pay-001", "MERCHANT",
                        null, "PAY-x", "corr-5"));
        assertEquals(422, ex.getStatusCode().value());
    }
}
