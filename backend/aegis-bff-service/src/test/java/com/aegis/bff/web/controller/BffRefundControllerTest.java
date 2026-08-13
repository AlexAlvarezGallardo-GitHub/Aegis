package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.JwtSigningKey;
import com.aegis.bff.domain.port.PaymentExecutionClient;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.infrastructure.security.JwtTokenValidator;
import com.aegis.bff.web.dto.CreateRefundRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.jsonwebtoken.security.Keys;

@DisplayName("BffRefundController")
class BffRefundControllerTest {

    private static final String TEST_SECRET = "aegis-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String USER_ID = "user-uuid-123";

    private PaymentExecutionClient paymentExecutionClient;
    private TokenValidator tokenValidator;
    private FakeSessionJwtStore sessionJwtStore;
    private MockMvc mockMvc;
    private String validAccessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        paymentExecutionClient = mock(PaymentExecutionClient.class);
        JwtSigningKey signingKey = () -> SECRET_KEY;
        tokenValidator = new JwtTokenValidator(signingKey);
        sessionJwtStore = new FakeSessionJwtStore(null);

        Instant now = Instant.now();
        validAccessToken = Jwts.builder()
                .subject(USER_ID)
                .claim("email", "john@example.com")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(SECRET_KEY)
                .compact();

        sessionJwtStore.setAccessToken(validAccessToken);

        BffRefundController controller = new BffRefundController(
                paymentExecutionClient, sessionJwtStore, tokenValidator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("POST /api/bff/payments/{paymentId}/refund - refundPayment")
    class RefundPayment {

        @Test
        @DisplayName("Should refund payment and return 200")
        void shouldRefundPayment() throws Exception {
            // Arrange
            JsonNode refundResponse = objectMapper.readTree(
                    """
                    {"refundId":"r1","paymentId":"p1","status":"COMPLETED","amount":25.50}
                    """);
            String paymentId = UUID.randomUUID().toString();
            when(paymentExecutionClient.refundPayment(eq(validAccessToken), eq(USER_ID),
                            eq(paymentId),
                            eq(new BigDecimal("25.50")), eq("Product returned"),
                            eq("REF-1"), anyString()))
                    .thenReturn(refundResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/payments/{paymentId}/refund", paymentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateRefundRequest(
                                    new BigDecimal("25.50"), "Product returned", "REF-1")))
                            .header("X-Correlation-Id", "corr-r1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.refundId").value("r1"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should refund payment with null amount (full refund)")
        void shouldRefundPaymentWithNullAmount() throws Exception {
            // Arrange
            JsonNode refundResponse = objectMapper.readTree(
                    """
                    {"refundId":"r2","paymentId":"p1","status":"COMPLETED"}
                    """);
            String paymentId = UUID.randomUUID().toString();
            when(paymentExecutionClient.refundPayment(eq(validAccessToken), eq(USER_ID),
                            eq(paymentId),
                            isNull(), isNull(),
                            eq("REF-2"), anyString()))
                    .thenReturn(refundResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/payments/{paymentId}/refund", paymentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateRefundRequest(
                                    null, null, "REF-2"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.refundId").value("r2"));
        }

        @Test
        @DisplayName("Should generate correlation id when header is missing")
        void shouldGenerateCorrelationIdWhenMissing() throws Exception {
            // Arrange
            JsonNode refundResponse = objectMapper.readTree(
                    """
                    {"refundId":"r3","paymentId":"p1","status":"COMPLETED"}
                    """);
            String paymentId = UUID.randomUUID().toString();
            when(paymentExecutionClient.refundPayment(eq(validAccessToken), eq(USER_ID),
                            eq(paymentId),
                            eq(new BigDecimal("10.00")), isNull(),
                            eq("REF-3"), anyString()))
                    .thenReturn(refundResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/payments/{paymentId}/refund", paymentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateRefundRequest(
                                    new BigDecimal("10.00"), null, "REF-3"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.refundId").value("r3"));
        }
    }

    @Nested
    @DisplayName("When not authenticated")
    class WhenNotAuthenticated {

        @Test
        @DisplayName("Should throw IllegalStateException when no access token in session")
        void shouldThrowWhenNoAccessToken() {
            // Arrange
            sessionJwtStore.setAccessToken(null);
            BffRefundController controller = new BffRefundController(
                    paymentExecutionClient, sessionJwtStore, tokenValidator);

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> controller.refundPayment("p1", new CreateRefundRequest(null, null, "REF-x"), "corr-x"));
        }
    }

    /**
     * Test double for SessionJwtStore that returns a configurable access token.
     */
    static class FakeSessionJwtStore extends SessionJwtStore {
        private String accessToken;

        FakeSessionJwtStore(String accessToken) {
            this.accessToken = accessToken;
        }

        void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        @Override
        public Optional<String> getAccessToken() {
            return Optional.ofNullable(accessToken);
        }

        @Override
        public Optional<String> getRefreshToken() {
            return Optional.empty();
        }

        @Override
        public void storeTokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
        }

        @Override
        public void clear() {
            this.accessToken = null;
        }
    }
}
