package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.JwtSigningKey;
import com.aegis.bff.domain.port.PaymentClient;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.infrastructure.security.JwtTokenValidator;
import com.aegis.bff.web.dto.CreateTransferRequest;
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

@DisplayName("BffTransferController")
class BffTransferControllerTest {

    private static final String TEST_SECRET = "aegis-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String USER_ID = "user-uuid-123";

    private PaymentClient paymentClient;
    private TokenValidator tokenValidator;
    private FakeSessionJwtStore sessionJwtStore;
    private MockMvc mockMvc;
    private String validAccessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        paymentClient = mock(PaymentClient.class);
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

        BffTransferController controller = new BffTransferController(
                paymentClient, sessionJwtStore, tokenValidator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("POST /api/bff/transfers - transferFunds")
    class TransferFunds {

        @Test
        @DisplayName("Should create transfer and return 201")
        void shouldCreateTransfer() throws Exception {
            // Arrange
            JsonNode transferResponse = objectMapper.readTree(
                    """
                    {"transferId":"t1","status":"PENDING","sourceWalletId":"w1","destWalletId":"w2"}
                    """);
            UUID src = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID dst = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(paymentClient.transferFunds(eq(validAccessToken), eq(USER_ID),
                            eq(src.toString()), eq(dst.toString()),
                            eq(new BigDecimal("25.50")), eq("USD"),
                            eq("payment"), eq("ref-1"), anyString()))
                    .thenReturn(transferResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateTransferRequest(
                                    src, dst, new BigDecimal("25.50"), "USD", "payment", "ref-1")))
                            .header("X-Correlation-Id", "corr-t1"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transferId").value("t1"))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should generate correlation id when header is missing")
        void shouldGenerateCorrelationIdWhenMissing() throws Exception {
            // Arrange
            JsonNode transferResponse = objectMapper.readTree(
                    """
                    {"transferId":"t2","status":"PENDING"}
                    """);
            UUID src = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID dst = UUID.fromString("22222222-2222-2222-2222-222222222222");
            when(paymentClient.transferFunds(eq(validAccessToken), eq(USER_ID),
                            eq(src.toString()), eq(dst.toString()),
                            eq(new BigDecimal("10.00")), eq("EUR"),
                            isNull(), eq("ref-2"), anyString()))
                    .thenReturn(transferResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateTransferRequest(
                                    src, dst, new BigDecimal("10.00"), "EUR", null, "ref-2"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.transferId").value("t2"));
        }
    }

    @Nested
    @DisplayName("GET /api/bff/transfers/{transferId} - getTransfer")
    class GetTransfer {

        @Test
        @DisplayName("Should get specific transfer")
        void shouldGetTransfer() throws Exception {
            // Arrange
            JsonNode transferResponse = objectMapper.readTree(
                    """
                    {"transferId":"t1","status":"COMPLETED","amount":25.50}
                    """);
            when(paymentClient.getTransfer(eq(validAccessToken), eq(USER_ID),
                            eq("t1"), anyString()))
                    .thenReturn(transferResponse);

            // Act & Assert
            mockMvc.perform(get("/api/bff/transfers/t1")
                            .header("X-Correlation-Id", "corr-t2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transferId").value("t1"))
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
            BffTransferController controller = new BffTransferController(
                    paymentClient, sessionJwtStore, tokenValidator);

            // Act & Assert
            assertThrows(IllegalStateException.class,
                    () -> controller.getTransfer("t1", "corr-x"));
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
