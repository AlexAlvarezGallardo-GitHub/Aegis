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

@DisplayName("RestPaymentExecutionClient - refundPayment")
class RestRefundExecutionClientTest {

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

    @DisplayName("refundPayment - should POST with bearer token, user id header, refund body and correlation id")
    @org.junit.jupiter.api.Test
    void shouldRefundPayment() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"refundId":"r1","paymentId":"p1","status":"COMPLETED","amount":25.50}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.refundPayment("access-token", "user-1",
                "p1", new BigDecimal("25.50"), "Product returned", "REF-1", "corr-1");

        // Assert
        assertEquals("r1", result.get("refundId").asText());
        assertEquals("COMPLETED", result.get("status").asText());

        RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/api/v1/payments/p1/refund", request.getPath());
        assertEquals("Bearer access-token", request.getHeader("Authorization"));
        assertEquals("user-1", request.getHeader("X-User-Id"));
        assertEquals("corr-1", request.getHeader("X-Correlation-Id"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("\"amount\":25.50"));
        assertTrue(body.contains("\"reason\":\"Product returned\""));
        assertTrue(body.contains("\"reference\":\"REF-1\""));
        // userId must NOT be in the body — it is sent as a header
        assertFalse(body.contains("\"userId\""));
    }

    @DisplayName("refundPayment - should omit amount and reason when null")
    @org.junit.jupiter.api.Test
    void shouldRefundPaymentWithoutOptionalFields() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setBody("""
                        {"refundId":"r2","paymentId":"p1","status":"COMPLETED"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act
        JsonNode result = client.refundPayment("access-token", "user-1",
                "p1", null, null, "REF-2", "corr-2");

        // Assert
        assertEquals("r2", result.get("refundId").asText());

        RecordedRequest request = server.takeRequest();
        String body = request.getBody().readUtf8();
        assertFalse(body.contains("amount"));
        assertFalse(body.contains("reason"));
        assertTrue(body.contains("\"reference\":\"REF-2\""));
    }

    @DisplayName("refundPayment - should propagate 4xx errors")
    @org.junit.jupiter.api.Test
    void shouldPropagate4xxOnRefundPayment() throws Exception {
        // Arrange
        server.enqueue(new MockResponse()
                .setResponseCode(409)
                .setBody("""
                        {"error":"PAYMENT_ALREADY_REFUNDED"}
                        """)
                .addHeader("Content-Type", "application/json"));

        // Act & Assert
        HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                () -> client.refundPayment("access-token", "user-1",
                        "p1", new BigDecimal("25.50"), "reason", "REF-x", "corr-3"));
        assertEquals(409, ex.getStatusCode().value());
    }
}
