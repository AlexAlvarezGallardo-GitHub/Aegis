package com.aegis.payment.web.controller;

import com.aegis.payment.application.dto.TransferResult;
import com.aegis.payment.domain.exception.FraudAssessmentUnavailableException;
import com.aegis.payment.domain.exception.FraudRejectedException;
import com.aegis.payment.domain.exception.TransferNotFoundException;
import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.model.TransferStatus;
import com.aegis.payment.domain.port.inbound.GetTransferUseCase;
import com.aegis.payment.domain.port.inbound.TransferFundsUseCase;
import com.aegis.payment.web.advice.PaymentExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
@Import({PaymentExceptionHandler.class, TransferControllerTest.TestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TransferController - REST Endpoints")
class TransferControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        TransferFundsUseCase transferFundsUseCase() {
            return mock(TransferFundsUseCase.class);
        }

        @Bean
        GetTransferUseCase getTransferUseCase() {
            return mock(GetTransferUseCase.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransferFundsUseCase transferFundsUseCase;

    @Autowired
    private GetTransferUseCase getTransferUseCase;

    @Autowired
    private ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    void resetMocks() {
        reset(transferFundsUseCase, getTransferUseCase);
    }

    @Nested
    @DisplayName("GET /api/v1/transfers/{transferId}")
    class GetTransfer {

        @Test
        @DisplayName("Should return 200 with transfer data")
        void shouldReturn200() throws Exception {
            UUID id = UUID.randomUUID();
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            Transfer transfer = Transfer.rehydrate(id, source, dest, user,
                    new BigDecimal("100.00"), "EUR", "desc", "ref-001",
                    TransferStatus.PENDING, null, null, null,
                    Instant.now(), Instant.now(), null);

            when(getTransferUseCase.findById(id)).thenReturn(transfer);

            mockMvc.perform(get("/api/v1/transfers/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.transferId").value(id.toString()))
                    .andExpect(jsonPath("$.sourceWalletId").value(source.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("Should return 404 when transfer not found")
        void shouldReturn404() throws Exception {
            UUID id = UUID.randomUUID();
            when(getTransferUseCase.findById(id)).thenThrow(new TransferNotFoundException(id));

            mockMvc.perform(get("/api/v1/transfers/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transfers")
    class CreateTransfer {

        @Test
        @DisplayName("Should return 201 with FRAUD_CHECK status when fraud APPROVE")
        void shouldReturn201OnFraudApprove() throws Exception {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            Transfer transfer = Transfer.rehydrate(UUID.randomUUID(), source, dest, user,
                    new BigDecimal("50.00"), "EUR", null, "ref-001",
                    TransferStatus.FRAUD_CHECK, null, null, null,
                    Instant.now(), Instant.now(), null);

            when(transferFundsUseCase.execute(any())).thenReturn(transfer);

            String body = """
                    {
                      "sourceWalletId": "%s",
                      "destWalletId": "%s",
                      "userId": "%s",
                      "amount": 50.00,
                      "currency": "EUR",
                      "reference": "ref-001"
                    }
                    """.formatted(source, dest, user);

            mockMvc.perform(post("/api/v1/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("FRAUD_CHECK"));
        }

        @Test
        @DisplayName("Should return 422 when fraud rejects transfer")
        void shouldReturn422OnFraudReject() throws Exception {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();

            when(transferFundsUseCase.execute(any())).thenThrow(
                    new FraudRejectedException(UUID.randomUUID()));

            String body = """
                    {
                      "sourceWalletId": "%s",
                      "destWalletId": "%s",
                      "userId": "%s",
                      "amount": 50.00,
                      "currency": "EUR",
                      "reference": "ref-001"
                    }
                    """.formatted(source, dest, user);

            mockMvc.perform(post("/api/v1/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("TRANSFER_REJECTED_BY_FRAUD"));
        }

        @Test
        @DisplayName("Should return 503 when fraud service is unavailable")
        void shouldReturn503OnFraudUnavailable() throws Exception {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();

            when(transferFundsUseCase.execute(any())).thenThrow(
                    new FraudAssessmentUnavailableException(new RuntimeException("timeout")));

            String body = """
                    {
                      "sourceWalletId": "%s",
                      "destWalletId": "%s",
                      "userId": "%s",
                      "amount": 50.00,
                      "currency": "EUR",
                      "reference": "ref-001"
                    }
                    """.formatted(source, dest, user);

            mockMvc.perform(post("/api/v1/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value("FRAUD_UNAVAILABLE"));
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void shouldReturn400OnValidation() throws Exception {
            String body = """
                    {
                      "amount": 50.00
                    }
                    """;

            mockMvc.perform(post("/api/v1/transfers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
