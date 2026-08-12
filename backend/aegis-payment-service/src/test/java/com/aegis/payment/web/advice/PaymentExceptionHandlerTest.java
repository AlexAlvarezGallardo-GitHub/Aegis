package com.aegis.payment.web.advice;

import com.aegis.payment.domain.exception.DuplicatePaymentException;
import com.aegis.payment.domain.exception.DuplicateTransferException;
import com.aegis.payment.domain.exception.FraudAssessmentUnavailableException;
import com.aegis.payment.domain.exception.FraudRejectedException;
import com.aegis.payment.domain.exception.InvalidPaymentStateException;
import com.aegis.payment.domain.exception.InvalidTransferStateException;
import com.aegis.payment.domain.exception.PaymentAssessmentUnavailableException;
import com.aegis.payment.domain.exception.PaymentNotFoundException;
import com.aegis.payment.domain.exception.PaymentRejectedException;
import com.aegis.payment.domain.exception.PaymentSettlementFailedException;
import com.aegis.payment.domain.exception.SelfTransferException;
import com.aegis.payment.domain.exception.TransferNotFoundException;
import com.aegis.payment.domain.model.PaymentStatus;
import com.aegis.payment.domain.model.TransferStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentExceptionHandler - Exception Handling")
class PaymentExceptionHandlerTest {

    private PaymentExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PaymentExceptionHandler();
    }

    @Nested
    @DisplayName("When handling TransferNotFoundException")
    class WhenHandlingTransferNotFound {

        @Test
        @DisplayName("Should return 404 with TRANSFER_NOT_FOUND code")
        void shouldReturn404() {
            UUID id = UUID.randomUUID();
            TransferNotFoundException ex = new TransferNotFoundException(id);

            ResponseEntity<Map<String, Object>> response = handler.handleTransferNotFound(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("TRANSFER_NOT_FOUND", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling DuplicateTransferException")
    class WhenHandlingDuplicateTransfer {

        @Test
        @DisplayName("Should return 409 with TRANSFER_DUPLICATE code")
        void shouldReturn409() {
            DuplicateTransferException ex = new DuplicateTransferException("ref-001");

            ResponseEntity<Map<String, Object>> response = handler.handleDuplicateTransfer(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals("TRANSFER_DUPLICATE", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling SelfTransferException")
    class WhenHandlingSelfTransfer {

        @Test
        @DisplayName("Should return 400 with SELF_TRANSFER code")
        void shouldReturn400() {
            SelfTransferException ex = new SelfTransferException(UUID.randomUUID());

            ResponseEntity<Map<String, Object>> response = handler.handleSelfTransfer(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("SELF_TRANSFER", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling InvalidTransferStateException")
    class WhenHandlingInvalidState {

        @Test
        @DisplayName("Should return 400 with INVALID_TRANSFER_STATE code")
        void shouldReturn400() {
            InvalidTransferStateException ex =
                    new InvalidTransferStateException(TransferStatus.PENDING, TransferStatus.COMPLETED);

            ResponseEntity<Map<String, Object>> response = handler.handleInvalidState(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("INVALID_TRANSFER_STATE", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling FraudRejectedException")
    class WhenHandlingFraudRejected {

        @Test
        @DisplayName("Should return 422 with TRANSFER_REJECTED_BY_FRAUD code")
        void shouldReturn422() {
            FraudRejectedException ex = new FraudRejectedException(UUID.randomUUID());

            ResponseEntity<Map<String, Object>> response = handler.handleFraudRejected(ex);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals("TRANSFER_REJECTED_BY_FRAUD", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling FraudAssessmentUnavailableException")
    class WhenHandlingFraudUnavailable {

        @Test
        @DisplayName("Should return 503 with FRAUD_UNAVAILABLE code")
        void shouldReturn503() {
            FraudAssessmentUnavailableException ex =
                    new FraudAssessmentUnavailableException(new RuntimeException("timeout"));

            ResponseEntity<Map<String, Object>> response = handler.handleFraudUnavailable(ex);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals("FRAUD_UNAVAILABLE", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling UnsupportedOperationException")
    class WhenHandlingNotImplemented {

        @Test
        @DisplayName("Should return 501 with NOT_IMPLEMENTED code")
        void shouldReturn501() {
            UnsupportedOperationException ex =
                    new UnsupportedOperationException("Saga orchestration lands in #249-#251");

            ResponseEntity<Map<String, Object>> response = handler.handleNotImplemented(ex);

            assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
            assertEquals("NOT_IMPLEMENTED", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling PaymentNotFoundException")
    class WhenHandlingPaymentNotFound {

        @Test
        @DisplayName("Should return 404 with PAYMENT_NOT_FOUND code")
        void shouldReturn404() {
            PaymentNotFoundException ex = new PaymentNotFoundException(UUID.randomUUID());

            ResponseEntity<Map<String, Object>> response = handler.handlePaymentNotFound(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("PAYMENT_NOT_FOUND", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling DuplicatePaymentException")
    class WhenHandlingDuplicatePayment {

        @Test
        @DisplayName("Should return 409 with PAYMENT_DUPLICATE code")
        void shouldReturn409() {
            DuplicatePaymentException ex = new DuplicatePaymentException("PAY-001");

            ResponseEntity<Map<String, Object>> response = handler.handleDuplicatePayment(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals("PAYMENT_DUPLICATE", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling InvalidPaymentStateException")
    class WhenHandlingInvalidPaymentState {

        @Test
        @DisplayName("Should return 400 with INVALID_PAYMENT_STATE code")
        void shouldReturn400() {
            InvalidPaymentStateException ex =
                    new InvalidPaymentStateException(PaymentStatus.PENDING, PaymentStatus.COMPLETED);

            ResponseEntity<Map<String, Object>> response = handler.handleInvalidPaymentState(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("INVALID_PAYMENT_STATE", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling PaymentRejectedException")
    class WhenHandlingPaymentRejected {

        @Test
        @DisplayName("Should return 422 with PAYMENT_REJECTED_BY_FRAUD code")
        void shouldReturn422() {
            PaymentRejectedException ex = new PaymentRejectedException(UUID.randomUUID());

            ResponseEntity<Map<String, Object>> response = handler.handlePaymentRejected(ex);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals("PAYMENT_REJECTED_BY_FRAUD", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling PaymentAssessmentUnavailableException")
    class WhenHandlingPaymentAssessmentUnavailable {

        @Test
        @DisplayName("Should return 503 with FRAUD_UNAVAILABLE code")
        void shouldReturn503() {
            PaymentAssessmentUnavailableException ex =
                    new PaymentAssessmentUnavailableException("timeout", new RuntimeException());

            ResponseEntity<Map<String, Object>> response = handler.handlePaymentAssessmentUnavailable(ex);

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertEquals("FRAUD_UNAVAILABLE", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling PaymentSettlementFailedException")
    class WhenHandlingPaymentSettlementFailed {

        @Test
        @DisplayName("Should return 422 with SETTLEMENT_FAILED code")
        void shouldReturn422() {
            PaymentSettlementFailedException ex = new PaymentSettlementFailedException("wallet down");

            ResponseEntity<Map<String, Object>> response = handler.handlePaymentSettlementFailed(ex);

            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
            assertEquals("SETTLEMENT_FAILED", response.getBody().get("code"));
        }
    }

    @Nested
    @DisplayName("When handling inherited exceptions")
    class WhenHandlingInherited {

        @Test
        @DisplayName("Should handle IllegalArgumentException with 400")
        void shouldHandleIllegalArgument() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid field");

            ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("FIELD_REQUIRED", response.getBody().get("code"));
        }

        @Test
        @DisplayName("Should handle generic Exception with 500")
        void shouldHandleGeneric() {
            Exception ex = new RuntimeException("Unexpected error");

            ResponseEntity<Map<String, Object>> response = handler.handleGeneric(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("INTERNAL_ERROR", response.getBody().get("code"));
        }
    }
}
