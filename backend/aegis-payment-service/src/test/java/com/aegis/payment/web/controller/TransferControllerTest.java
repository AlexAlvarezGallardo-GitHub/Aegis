package com.aegis.payment.web.controller;

import com.aegis.payment.application.dto.TransferResult;
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
        @DisplayName("Should return 501 (scaffold — saga not implemented)")
        void shouldReturn501() throws Exception {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            Transfer transfer = Transfer.request(source, dest, user,
                    new BigDecimal("50.00"), "EUR", null, "ref-001");

            when(transferFundsUseCase.execute(any())).thenThrow(
                    new UnsupportedOperationException("Saga orchestration lands in #249-#251"));

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
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
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
