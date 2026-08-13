package com.aegis.payment.web.advice;

import com.aegis.common.web.advice.AbstractExceptionHandler;
import com.aegis.payment.domain.exception.DuplicatePaymentException;
import com.aegis.payment.domain.exception.DuplicateTransferException;
import com.aegis.payment.domain.exception.FraudAssessmentUnavailableException;
import com.aegis.payment.domain.exception.FraudRejectedException;
import com.aegis.payment.domain.exception.InvalidPaymentStateException;
import com.aegis.payment.domain.exception.InvalidRefundStateException;
import com.aegis.payment.domain.exception.InvalidTransferStateException;
import com.aegis.payment.domain.exception.PaymentAlreadyRefundedException;
import com.aegis.payment.domain.exception.PaymentAssessmentUnavailableException;
import com.aegis.payment.domain.exception.PaymentNotFoundException;
import com.aegis.payment.domain.exception.PaymentNotOwnedException;
import com.aegis.payment.domain.exception.PaymentNotRefundableException;
import com.aegis.payment.domain.exception.PaymentRejectedException;
import com.aegis.payment.domain.exception.PaymentSettlementFailedException;
import com.aegis.payment.domain.exception.RefundAlreadyExistsException;
import com.aegis.payment.domain.exception.RefundExceedsPaymentException;
import com.aegis.payment.domain.exception.RefundNotFoundException;
import com.aegis.payment.domain.exception.SelfTransferException;
import com.aegis.payment.domain.exception.TransferNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Payment-service specific exception handler. Extends the shared {@link AbstractExceptionHandler}
 * and adds domain-specific handlers for payment, transfer, and refund exceptions.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class PaymentExceptionHandler extends AbstractExceptionHandler {

    // --- Transfer exceptions ---

    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTransferNotFound(TransferNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateTransferException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateTransfer(DuplicateTransferException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(SelfTransferException.class)
    public ResponseEntity<Map<String, Object>> handleSelfTransfer(SelfTransferException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(InvalidTransferStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(InvalidTransferStateException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(FraudRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleFraudRejected(FraudRejectedException ex) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(FraudAssessmentUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleFraudUnavailable(FraudAssessmentUnavailableException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getCode(), ex.getMessage(), null);
    }

    // --- Payment exceptions ---

    /**
     * Handles {@link PaymentNotFoundException} with HTTP 404.
     */
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotFound(PaymentNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link DuplicatePaymentException} with HTTP 409.
     */
    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicatePayment(DuplicatePaymentException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link InvalidPaymentStateException} with HTTP 400.
     */
    @ExceptionHandler(InvalidPaymentStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPaymentState(InvalidPaymentStateException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link PaymentRejectedException} with HTTP 422.
     */
    @ExceptionHandler(PaymentRejectedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentRejected(PaymentRejectedException ex) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link PaymentAssessmentUnavailableException} with HTTP 503.
     */
    @ExceptionHandler(PaymentAssessmentUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentAssessmentUnavailable(
            PaymentAssessmentUnavailableException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link PaymentSettlementFailedException} with HTTP 422.
     */
    @ExceptionHandler(PaymentSettlementFailedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentSettlementFailed(
            PaymentSettlementFailedException ex) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), null);
    }

    // --- Refund exceptions ---

    /**
     * Handles {@link RefundNotFoundException} with HTTP 404.
     */
    @ExceptionHandler(RefundNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRefundNotFound(RefundNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link RefundAlreadyExistsException} with HTTP 409.
     */
    @ExceptionHandler(RefundAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleRefundAlreadyExists(RefundAlreadyExistsException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link PaymentAlreadyRefundedException} with HTTP 409.
     */
    @ExceptionHandler(PaymentAlreadyRefundedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentAlreadyRefunded(PaymentAlreadyRefundedException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link PaymentNotRefundableException} with HTTP 422.
     */
    @ExceptionHandler(PaymentNotRefundableException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotRefundable(PaymentNotRefundableException ex) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link PaymentNotOwnedException} with HTTP 403.
     */
    @ExceptionHandler(PaymentNotOwnedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentNotOwned(PaymentNotOwnedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link RefundExceedsPaymentException} with HTTP 422.
     */
    @ExceptionHandler(RefundExceedsPaymentException.class)
    public ResponseEntity<Map<String, Object>> handleRefundExceedsPayment(RefundExceedsPaymentException ex) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link InvalidRefundStateException} with HTTP 400.
     */
    @ExceptionHandler(InvalidRefundStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRefundState(InvalidRefundStateException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleNotImplemented(UnsupportedOperationException ex) {
        return buildErrorResponse(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", ex.getMessage(), null);
    }
}
