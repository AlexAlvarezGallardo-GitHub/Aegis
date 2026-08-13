package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.JwtSigningKey;
import com.aegis.bff.domain.port.PaymentExecutionClient;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.infrastructure.security.JwtTokenValidator;
import com.aegis.bff.web.dto.CreatePaymentRequest;
import com.aegis.bff.web.dto.PayeeRequest;
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

@DisplayName("BffPaymentController")
class BffPaymentControllerTest {

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

        BffPaymentController controller = new BffPaymentController(
                paymentExecutionClient, sessionJwtStore, tokenValidator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("POST /api/bff/payments - executePayment")
    class ExecutePayment {

        @Test
        @DisplayName("Should create payment and return 201")
        void shouldCreatePayment() throws Exception {
            // Arrange
            JsonNode paymentResponse = objectMapper.readTree(
                    """
                    {"paymentId":"p1","status":"COMPLETED","walletId":"w1","amount":25.50}
                    """);
            UUID walletId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            PayeeRequest payee = new PayeeRequest("Acme Corp", "acme-001", "MERCHANT");
            when(paymentExecutionClient.executePayment(eq(validAccessToken), eq(USER_ID),
                            eq(walletId.toString()),
                            eq(new BigDecimal("25.50")), eq("USD"),
                            eq("Acme Corp"), eq("acme-001"), eq("MERCHANT"),
                            eq("purchase"), eq("PAY-1"), anyString()))
                    .thenReturn(paymentResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreatePaymentRequest(
                                    walletId, new BigDecimal("25.50"), "USD", payee, "purchase", "PAY-1")))
                            .header("X-Correlation-Id", "corr-p1"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value("p1"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should generate correlation id when header is missing")
        void shouldGenerateCorrelationIdWhenMissing() throws Exception {
            // Arrange
            JsonNode paymentResponse = objectMapper.readTree(
                    """
                    {"paymentId":"p2","status":"COMPLETED"}
                    """);
            UUID walletId = UUID.fromString("11111111-1111-1111-1111-111111111111");
            PayeeRequest payee = new PayeeRequest("Store", "store-002", "MERCHANT");
            when(paymentExecutionClient.executePayment(eq(validAccessToken), eq(USER_ID),
                            eq(walletId.toString()),
                            eq(new BigDecimal("10.00")), eq("EUR"),
                            eq("Store"), eq("store-002"), eq("MERCHANT"),
                            isNull(), eq("PAY-2"), anyString()))
                    .thenReturn(paymentResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreatePaymentRequest(
                                    walletId, new BigDecimal("10.00"), "EUR", payee, null, "PAY-2"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentId").value("p2"));
        }
    }

    @Nested
    @DisplayName("GET /api/bff/payments/{paymentId} - getPayment")
    class GetPayment {

        @Test
        @DisplayName("Should get specific payment")
        void shouldGetPayment() throws Exception {
            // Arrange
            JsonNode paymentResponse = objectMapper.readTree(
                    """
                    {"paymentId":"p1","status":"COMPLETED","amount":25.50}
                    """);
            when(paymentExecutionClient.getPayment(eq(validAccessToken), eq(USER_ID),
                            eq("p1"), anyString()))
                    .thenReturn(paymentResponse);

            // Act & Assert
            mockMvc.perform(get("/api/bff/payments/p1")
                            .header("X-Correlation-Id", "corr-p2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value("p1"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
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
            BffPaymentController controller = new BffPaymentController(
                    paymentExecutionClient, sessionJwtStore, tokenValidator);

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> controller.getPayment("p1", "corr-x"));
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
