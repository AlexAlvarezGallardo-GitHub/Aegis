package com.aegis.bff.web.controller;

import com.aegis.bff.application.service.SessionJwtStore;
import com.aegis.bff.domain.port.JwtSigningKey;
import com.aegis.bff.domain.port.TokenValidator;
import com.aegis.bff.domain.port.WalletClient;
import com.aegis.bff.infrastructure.security.JwtTokenValidator;
import com.aegis.bff.web.dto.AdjustBalanceRequest;
import com.aegis.bff.web.dto.CreateWalletRequest;
import com.aegis.bff.web.dto.DepositFundsRequest;
import com.aegis.bff.web.dto.UpdateStatusRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BffWalletController")
class BffWalletControllerTest {

    private static final String TEST_SECRET = "aegis-dev-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    private static final String USER_ID = "user-uuid-123";

    private WalletClient walletClient;
    private TokenValidator tokenValidator;
    private FakeSessionJwtStore sessionJwtStore;
    private MockMvc mockMvc;
    private String validAccessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        walletClient = mock(WalletClient.class);
        JwtSigningKey signingKey = () -> SECRET_KEY;
        tokenValidator = new JwtTokenValidator(signingKey);
        sessionJwtStore = new FakeSessionJwtStore(null);

        // Generate a real valid JWT
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

        BffWalletController controller = new BffWalletController(
                walletClient, sessionJwtStore, tokenValidator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("POST /api/bff/wallets - createWallet")
    class CreateWallet {

        @Test
        @DisplayName("Should create wallet and return 201")
        void shouldCreateWallet() throws Exception {
            // Arrange
            JsonNode walletResponse = objectMapper.readTree(
                    """
                    {"walletId":"w1","currency":"USD","balance":0,"status":"ACTIVE"}
                    """);
            when(walletClient.createWallet(eq(validAccessToken), eq(USER_ID), eq("USD"), anyString()))
                    .thenReturn(walletResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateWalletRequest("USD")))
                            .header("X-Correlation-Id", "corr-1"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.walletId").value("w1"))
                    .andExpect(jsonPath("$.currency").value("USD"));
        }

        @Test
        @DisplayName("Should generate correlation id when header is missing")
        void shouldGenerateCorrelationIdWhenMissing() throws Exception {
            // Arrange
            JsonNode walletResponse = objectMapper.readTree("""
                    {"walletId":"w2","currency":"EUR"}
                    """);
            when(walletClient.createWallet(eq(validAccessToken), eq(USER_ID), eq("EUR"), anyString()))
                    .thenReturn(walletResponse);

            // Act & Assert
            mockMvc.perform(post("/api/bff/wallets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new CreateWalletRequest("EUR"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.walletId").value("w2"));
        }
    }

    @Nested
    @DisplayName("GET /api/bff/wallets - listWallets")
    class ListWallets {

        @Test
        @DisplayName("Should list wallets")
        void shouldListWallets() throws Exception {
            // Arrange
            JsonNode walletsResponse = objectMapper.readTree(
                    """
                    [{"walletId":"w1"},{"walletId":"w2"}]
                    """);
            when(walletClient.listWallets(validAccessToken, USER_ID)).thenReturn(walletsResponse);

            // Act & Assert
            mockMvc.perform(get("/api/bff/wallets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].walletId").value("w1"))
                    .andExpect(jsonPath("$[1].walletId").value("w2"));
        }
    }

    @Nested
    @DisplayName("GET /api/bff/wallets/{walletId} - getWallet")
    class GetWallet {

        @Test
        @DisplayName("Should get specific wallet")
        void shouldGetWallet() throws Exception {
            // Arrange
            JsonNode walletResponse = objectMapper.readTree(
                    """
                    {"walletId":"w1","currency":"USD","balance":100}
                    """);
            when(walletClient.getWallet(validAccessToken, USER_ID, "w1")).thenReturn(walletResponse);

            // Act & Assert
            mockMvc.perform(get("/api/bff/wallets/w1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.walletId").value("w1"))
                    .andExpect(jsonPath("$.balance").value(100));
        }
    }

    @Nested
    @DisplayName("PATCH /api/bff/wallets/{walletId}/balance - adjustBalance")
    class AdjustBalance {

        @Test
        @DisplayName("Should adjust balance and return 200")
        void shouldAdjustBalance() throws Exception {
            // Arrange
            JsonNode response = objectMapper.readTree("""
                    {"walletId":"w1","balance":150}
                    """);
            when(walletClient.adjustBalance(eq(validAccessToken), eq(USER_ID), eq("w1"),
                    eq("CREDIT"), eq(new BigDecimal("50.00")), eq("bonus"), anyString()))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(patch("/api/bff/wallets/w1/balance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AdjustBalanceRequest("CREDIT", new BigDecimal("50.00"), "bonus")))
                            .header("X-Correlation-Id", "corr-2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(150));
        }
    }

    @Nested
    @DisplayName("POST /api/bff/wallets/{walletId}/deposits - depositFunds")
    class DepositFunds {

        @Test
        @DisplayName("Should deposit funds and return 201")
        void shouldDepositFunds() throws Exception {
            // Arrange
            JsonNode response = objectMapper.readTree(
                    """
                    {"walletId":"w1","depositId":"d1","status":"COMPLETED"}
                    """);
            when(walletClient.depositFunds(eq(validAccessToken), eq(USER_ID), eq("w1"),
                    eq(new BigDecimal("100.00")), eq("BANK_TRANSFER"), eq("ref-1"), anyString()))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/bff/wallets/w1/deposits")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new DepositFundsRequest(new BigDecimal("100.00"), "BANK_TRANSFER", "ref-1")))
                            .header("X-Correlation-Id", "corr-3"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }
    }

    @Nested
    @DisplayName("PATCH /api/bff/wallets/{walletId}/status - updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("Should update wallet status")
        void shouldUpdateStatus() throws Exception {
            // Arrange
            JsonNode response = objectMapper.readTree("""
                    {"walletId":"w1","status":"FROZEN"}
                    """);
            when(walletClient.updateStatus(validAccessToken, USER_ID, "w1", "FROZEN"))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(patch("/api/bff/wallets/w1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateStatusRequest("FROZEN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FROZEN"));
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
            BffWalletController controller = new BffWalletController(
                    walletClient, sessionJwtStore, tokenValidator);

            // Act & Assert
            assertThrows(IllegalStateException.class, () -> controller.listWallets());
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
