package com.aegis.payment.web.controller;

import com.aegis.payment.application.dto.PaymentResult;
import com.aegis.payment.domain.exception.PaymentAssessmentUnavailableException;
import com.aegis.payment.domain.exception.PaymentNotFoundException;
import com.aegis.payment.domain.exception.PaymentRejectedException;
import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.PayeeType;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.model.PaymentStatus;
import com.aegis.payment.domain.port.inbound.ExecutePaymentUseCase;
import com.aegis.payment.domain.port.inbound.GetPaymentUseCase;
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

@WebMvcTest(PaymentController.class)
@Import({PaymentExceptionHandler.class, PaymentControllerTest.TestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PaymentController - REST Endpoints")
class PaymentControllerTest {

    private static final Payee PAYEE = new Payee("Cafe Central", "merchant-123", PayeeType.MERCHANT);

    @TestConfiguration
    static class TestConfig {
        @Bean
        ExecutePaymentUseCase executePaymentUseCase() {
            return mock(ExecutePaymentUseCase.class);
        }

        @Bean
        GetPaymentUseCase getPaymentUseCase() {
            return mock(GetPaymentUseCase.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ExecutePaymentUseCase executePaymentUseCase;
    @Autowired private GetPaymentUseCase getPaymentUseCase;
    @Autowired private ObjectMapper objectMapper;

    @org.junit.jupiter.api.BeforeEach
    void resetMocks() {
        reset(executePaymentUseCase, getPaymentUseCase);
    }

    @Nested
    @DisplayName("POST /api/v1/payments")
    class CreatePayment {

        @Test
        @DisplayName("Should return 201 with COMPLETED status on success")
        void shouldReturn201OnSuccess() throws Exception {
            UUID wallet = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            Payment payment = Payment.rehydrate(UUID.randomUUID(), wallet, user,
                    new BigDecimal("25.00"), "EUR", PAYEE, "Coffee", "PAY-001",
                    PaymentStatus.COMPLETED, UUID.randomUUID(), UUID.randomUUID(), null,
                    Instant.now(), Instant.now(), Instant.now());

            when(executePaymentUseCase.execute(any())).thenReturn(payment);

            String body = """
                    {
                      "walletId": "%s",
                      "amount": 25.00,
                      "currency": "EUR",
                      "payee": {"name": "Cafe Central", "id": "merchant-123", "type": "MERCHANT"},
                      "description": "Coffee",
                      "reference": "PAY-001"
                    }
                    """.formatted(wallet);

            mockMvc.perform(post("/api/v1/payments")
                            .header("X-User-Id", user.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should return 422 when fraud rejects payment")
        void shouldReturn422OnFraudReject() throws Exception {
            when(executePaymentUseCase.execute(any())).thenThrow(
                    new PaymentRejectedException(UUID.randomUUID()));

            String body = """
                    {
                      "walletId": "%s",
                      "amount": 25.00,
                      "currency": "EUR",
                      "payee": {"name": "Cafe Central", "id": "merchant-123", "type": "MERCHANT"},
                      "reference": "PAY-001"
                    }
                    """.formatted(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/payments")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PAYMENT_REJECTED_BY_FRAUD"));
        }

        @Test
        @DisplayName("Should return 503 when fraud service is unavailable")
        void shouldReturn503OnFraudUnavailable() throws Exception {
            when(executePaymentUseCase.execute(any())).thenThrow(
                    new PaymentAssessmentUnavailableException("timeout", new RuntimeException()));

            String body = """
                    {
                      "walletId": "%s",
                      "amount": 25.00,
                      "currency": "EUR",
                      "payee": {"name": "Cafe Central", "id": "merchant-123", "type": "MERCHANT"},
                      "reference": "PAY-001"
                    }
                    """.formatted(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/payments")
                            .header("X-User-Id", UUID.randomUUID().toString())
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
                      "amount": 25.00
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/payments/{paymentId}")
    class GetPayment {

        @Test
        @DisplayName("Should return 200 with payment data")
        void shouldReturn200() throws Exception {
            UUID id = UUID.randomUUID();
            UUID wallet = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            Payment payment = Payment.rehydrate(id, wallet, user,
                    new BigDecimal("25.00"), "EUR", PAYEE, null, "PAY-001",
                    PaymentStatus.COMPLETED, null, null, null,
                    Instant.now(), Instant.now(), Instant.now());

            when(getPaymentUseCase.findById(id)).thenReturn(payment);

            mockMvc.perform(get("/api/v1/payments/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentId").value(id.toString()))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should return 404 when payment not found")
        void shouldReturn404() throws Exception {
            UUID id = UUID.randomUUID();
            when(getPaymentUseCase.findById(id)).thenThrow(new PaymentNotFoundException(id));

            mockMvc.perform(get("/api/v1/payments/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        }
    }
}
