package com.aegis.payment.web.controller;

import com.aegis.payment.application.dto.RefundResult;
import com.aegis.payment.domain.exception.PaymentAlreadyRefundedException;
import com.aegis.payment.domain.exception.PaymentNotFoundException;
import com.aegis.payment.domain.exception.PaymentNotOwnedException;
import com.aegis.payment.domain.exception.PaymentNotRefundableException;
import com.aegis.payment.domain.exception.RefundExceedsPaymentException;
import com.aegis.payment.domain.model.Refund;
import com.aegis.payment.domain.model.RefundStatus;
import com.aegis.payment.domain.port.inbound.RefundPaymentUseCase;
import com.aegis.payment.web.advice.PaymentExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RefundController.class)
@Import({PaymentExceptionHandler.class, RefundControllerTest.TestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("RefundController - REST Endpoints")
class RefundControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        RefundPaymentUseCase refundPaymentUseCase() {
            return mock(RefundPaymentUseCase.class);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private RefundPaymentUseCase refundPaymentUseCase;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void resetMocks() {
        reset(refundPaymentUseCase);
    }

    private Refund completedRefund(UUID paymentId, UUID walletId, UUID userId) {
        return Refund.rehydrate(UUID.randomUUID(), paymentId, walletId, userId,
                new BigDecimal("25.00"), "EUR", "Product returned", "REF-001",
                RefundStatus.COMPLETED, Instant.now(), Instant.now(), Instant.now());
    }

    @Nested
    @DisplayName("POST /api/v1/payments/{paymentId}/refund")
    class RefundPayment {

        @Test
        @DisplayName("Should return 200 with COMPLETED status on success")
        void shouldReturn200OnSuccess() throws Exception {
            UUID paymentId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Refund refund = completedRefund(paymentId, walletId, userId);

            when(refundPaymentUseCase.refund(any())).thenReturn(RefundResult.from(refund));

            String body = """
                    {
                      "amount": 25.00,
                      "reason": "Product returned",
                      "reference": "REF-001"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .header("X-User-Id", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.paymentId").value(paymentId.toString()));
        }

        @Test
        @DisplayName("Should return 404 when payment not found")
        void shouldReturn404WhenPaymentNotFound() throws Exception {
            UUID paymentId = UUID.randomUUID();
            when(refundPaymentUseCase.refund(any())).thenThrow(new PaymentNotFoundException(paymentId));

            String body = """
                    {
                      "reference": "REF-001"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 409 when payment already refunded")
        void shouldReturn409WhenAlreadyRefunded() throws Exception {
            UUID paymentId = UUID.randomUUID();
            when(refundPaymentUseCase.refund(any())).thenThrow(new PaymentAlreadyRefundedException(paymentId));

            String body = """
                    {
                      "reference": "REF-001"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PAYMENT_ALREADY_REFUNDED"));
        }

        @Test
        @DisplayName("Should return 403 when payment not owned by user")
        void shouldReturn403WhenNotOwned() throws Exception {
            UUID paymentId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            when(refundPaymentUseCase.refund(any())).thenThrow(new PaymentNotOwnedException(paymentId, userId));

            String body = """
                    {
                      "reference": "REF-001"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .header("X-User-Id", userId.toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_OWNED"));
        }

        @Test
        @DisplayName("Should return 422 when payment not refundable")
        void shouldReturn422WhenNotRefundable() throws Exception {
            UUID paymentId = UUID.randomUUID();
            when(refundPaymentUseCase.refund(any())).thenThrow(new PaymentNotRefundableException(paymentId));

            String body = """
                    {
                      "reference": "REF-001"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PAYMENT_NOT_REFUNDABLE"));
        }

        @Test
        @DisplayName("Should return 422 when refund exceeds payment")
        void shouldReturn422WhenExceeds() throws Exception {
            UUID paymentId = UUID.randomUUID();
            when(refundPaymentUseCase.refund(any())).thenThrow(new RefundExceedsPaymentException(paymentId));

            String body = """
                    {
                      "amount": 50.00,
                      "reference": "REF-001"
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/" + paymentId + "/refund")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("REFUND_EXCEEDS_PAYMENT"));
        }

        @Test
        @DisplayName("Should return 400 when reference is missing")
        void shouldReturn400WhenReferenceMissing() throws Exception {
            String body = """
                    {
                      "amount": 25.00
                    }
                    """;

            mockMvc.perform(post("/api/v1/payments/" + UUID.randomUUID() + "/refund")
                            .header("X-User-Id", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }
}
