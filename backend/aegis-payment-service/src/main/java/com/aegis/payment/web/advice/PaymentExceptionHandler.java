package com.aegis.payment.web.advice;

import com.aegis.common.web.advice.AbstractExceptionHandler;
import com.aegis.payment.domain.exception.DuplicateTransferException;
import com.aegis.payment.domain.exception.InvalidTransferStateException;
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
 * and adds domain-specific handlers for payment exceptions.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class PaymentExceptionHandler extends AbstractExceptionHandler {

    /**
     * Handles {@link TransferNotFoundException} with HTTP 404.
     *
     * @param ex the exception
     * @return the error response envelope
     */
    @ExceptionHandler(TransferNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleTransferNotFound(TransferNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link DuplicateTransferException} with HTTP 409.
     *
     * @param ex the exception
     * @return the error response envelope
     */
    @ExceptionHandler(DuplicateTransferException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateTransfer(DuplicateTransferException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link SelfTransferException} with HTTP 400.
     *
     * @param ex the exception
     * @return the error response envelope
     */
    @ExceptionHandler(SelfTransferException.class)
    public ResponseEntity<Map<String, Object>> handleSelfTransfer(SelfTransferException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link InvalidTransferStateException} with HTTP 400.
     *
     * @param ex the exception
     * @return the error response envelope
     */
    @ExceptionHandler(InvalidTransferStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(InvalidTransferStateException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), null);
    }

    /**
     * Handles {@link UnsupportedOperationException} with HTTP 501.
     *
     * @param ex the exception
     * @return the error response envelope
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, Object>> handleNotImplemented(UnsupportedOperationException ex) {
        return buildErrorResponse(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", ex.getMessage(), null);
    }
}
