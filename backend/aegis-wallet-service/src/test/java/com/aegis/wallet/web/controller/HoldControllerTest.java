package com.aegis.wallet.web.controller;

import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.HoldNotActiveException;
import com.aegis.wallet.domain.exception.HoldNotFoundException;
import com.aegis.wallet.domain.exception.InsufficientFundsException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.port.inbound.CreateHoldUseCase;
import com.aegis.wallet.domain.port.inbound.ReleaseHoldUseCase;
import com.aegis.wallet.domain.port.inbound.SettleTransferUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HoldController.class)
@AutoConfigureMockMvc(addFilters = false)
class HoldControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CreateHoldUseCase createHoldUseCase;
    @MockBean private SettleTransferUseCase settleTransferUseCase;
    @MockBean private ReleaseHoldUseCase releaseHoldUseCase;

    @Nested
    @DisplayName("POST /api/v1/wallets/{walletId}/holds")
    class CreateHoldEndpoint {

        @Test
        void shouldReturn201OnSuccess() throws Exception {
            UUID walletId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            Instant now = Instant.now();

            when(createHoldUseCase.createHold(any())).thenReturn(new CreateHoldUseCase.HoldResult(
                    holdId, walletId, new BigDecimal("100.00"), "EUR", "TXN-1",
                    "ACTIVE", new BigDecimal("400.00"), now, now.plusSeconds(300)));

            String body = """
                    {"amount": 100.00, "currency": "EUR", "reference": "TXN-1"}
                    """;

            mockMvc.perform(post("/api/v1/wallets/" + walletId + "/holds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.holdId").value(holdId.toString()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.availableBalance").value(400.00));
        }

        @Test
        void shouldReturn404WhenWalletNotFound() throws Exception {
            when(createHoldUseCase.createHold(any()))
                    .thenThrow(new WalletNotFoundException(UUID.randomUUID()));

            String body = """
                    {"amount": 100.00, "currency": "EUR", "reference": "TXN-1"}
                    """;

            mockMvc.perform(post("/api/v1/wallets/" + UUID.randomUUID() + "/holds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));
        }

        @Test
        void shouldReturn422WhenInsufficientFunds() throws Exception {
            when(createHoldUseCase.createHold(any()))
                    .thenThrow(new InsufficientFundsException("Insufficient available balance"));

            String body = """
                    {"amount": 100.00, "currency": "EUR", "reference": "TXN-1"}
                    """;

            mockMvc.perform(post("/api/v1/wallets/" + UUID.randomUUID() + "/holds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));
        }

        @Test
        void shouldReturn422WhenWalletNotActive() throws Exception {
            when(createHoldUseCase.createHold(any()))
                    .thenThrow(new WalletNotActiveException(UUID.randomUUID(), "FROZEN"));

            String body = """
                    {"amount": 100.00, "currency": "EUR", "reference": "TXN-1"}
                    """;

            mockMvc.perform(post("/api/v1/wallets/" + UUID.randomUUID() + "/holds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("WALLET_NOT_ACTIVE"));
        }

        @Test
        void shouldReturn422WhenCurrencyMismatch() throws Exception {
            when(createHoldUseCase.createHold(any()))
                    .thenThrow(new CurrencyMismatchException("EUR", "USD"));

            String body = """
                    {"amount": 100.00, "currency": "USD", "reference": "TXN-1"}
                    """;

            mockMvc.perform(post("/api/v1/wallets/" + UUID.randomUUID() + "/holds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));
        }

        @Test
        void shouldReturn400WhenAmountMissing() throws Exception {
            String body = """
                    {"currency": "EUR", "reference": "TXN-1"}
                    """;

            mockMvc.perform(post("/api/v1/wallets/" + UUID.randomUUID() + "/holds")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/wallets/transfers/settle")
    class SettleTransferEndpoint {

        @Test
        void shouldReturn200OnSuccess() throws Exception {
            UUID transferId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            UUID sourceId = UUID.randomUUID();
            UUID destId = UUID.randomUUID();
            Instant now = Instant.now();

            when(settleTransferUseCase.settle(any())).thenReturn(new SettleTransferUseCase.SettleResult(
                    transferId, holdId, sourceId, new BigDecimal("400.00"),
                    destId, new BigDecimal("100.00"), now));

            String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
                put("transferId", transferId.toString());
                put("holdId", holdId.toString());
                put("sourceWalletId", sourceId.toString());
                put("destWalletId", destId.toString());
                put("amount", 100.00);
                put("currency", "EUR");
            }});

            mockMvc.perform(post("/api/v1/wallets/transfers/settle")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transferId").value(transferId.toString()))
                    .andExpect(jsonPath("$.sourceNewBalance").value(400.00))
                    .andExpect(jsonPath("$.destNewBalance").value(100.00));
        }

        @Test
        void shouldReturn404WhenHoldNotFound() throws Exception {
            when(settleTransferUseCase.settle(any()))
                    .thenThrow(new HoldNotFoundException(UUID.randomUUID()));

            String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
                put("transferId", UUID.randomUUID().toString());
                put("holdId", UUID.randomUUID().toString());
                put("sourceWalletId", UUID.randomUUID().toString());
                put("destWalletId", UUID.randomUUID().toString());
                put("amount", 100.00);
                put("currency", "EUR");
            }});

            mockMvc.perform(post("/api/v1/wallets/transfers/settle")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("HOLD_NOT_FOUND"));
        }

        @Test
        void shouldReturn409WhenHoldNotActive() throws Exception {
            when(settleTransferUseCase.settle(any()))
                    .thenThrow(new HoldNotActiveException("Hold is SETTLED"));

            String body = objectMapper.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
                put("transferId", UUID.randomUUID().toString());
                put("holdId", UUID.randomUUID().toString());
                put("sourceWalletId", UUID.randomUUID().toString());
                put("destWalletId", UUID.randomUUID().toString());
                put("amount", 100.00);
                put("currency", "EUR");
            }});

            mockMvc.perform(post("/api/v1/wallets/transfers/settle")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("HOLD_NOT_ACTIVE"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/wallets/{walletId}/holds/{holdId}/release")
    class ReleaseHoldEndpoint {

        @Test
        void shouldReturn200OnSuccess() throws Exception {
            UUID walletId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            Instant now = Instant.now();

            when(releaseHoldUseCase.release(any())).thenReturn(new ReleaseHoldUseCase.HoldResult(
                    holdId, walletId, new BigDecimal("100.00"), "EUR", "TXN-1",
                    "RELEASED", new BigDecimal("500.00"), now, now.plusSeconds(300)));

            mockMvc.perform(post("/api/v1/wallets/" + walletId + "/holds/" + holdId + "/release"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("RELEASED"));
        }

        @Test
        void shouldReturn404WhenHoldNotFound() throws Exception {
            when(releaseHoldUseCase.release(any()))
                    .thenThrow(new HoldNotFoundException(UUID.randomUUID()));

            mockMvc.perform(post("/api/v1/wallets/" + UUID.randomUUID()
                            + "/holds/" + UUID.randomUUID() + "/release"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("HOLD_NOT_FOUND"));
        }

        @Test
        void shouldReturn409WhenHoldNotActive() throws Exception {
            when(releaseHoldUseCase.release(any()))
                    .thenThrow(new HoldNotActiveException("Hold is SETTLED"));

            mockMvc.perform(post("/api/v1/wallets/" + UUID.randomUUID()
                            + "/holds/" + UUID.randomUUID() + "/release"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("HOLD_NOT_ACTIVE"));
        }
    }
}
