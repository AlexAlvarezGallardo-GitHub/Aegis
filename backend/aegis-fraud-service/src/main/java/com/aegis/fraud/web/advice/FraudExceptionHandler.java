package com.aegis.fraud.web.advice;

import com.aegis.common.web.advice.AbstractExceptionHandler;
import com.aegis.fraud.domain.exception.AssessmentNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Fraud-service specific exception handler. Extends the shared {@link AbstractExceptionHandler}
 * and adds domain-specific handlers for fraud exceptions.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class FraudExceptionHandler extends AbstractExceptionHandler {

    /**
     * Handles {@link AssessmentNotFoundException} with HTTP 404.
     *
     * @param ex the exception
     * @return the error response envelope
     */
    @ExceptionHandler(AssessmentNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAssessmentNotFound(AssessmentNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), null);
    }
}
