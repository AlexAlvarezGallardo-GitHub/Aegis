package com.aegis.bff.web.advice;

import com.aegis.common.web.advice.AbstractExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

/**
 * BFF-specific exception handler.
 *
 * <p>Extends {@link AbstractExceptionHandler} to inherit the standard error envelope
 * and generic handlers. Adds a handler that propagates downstream service error
 * responses through the BFF.</p>
 */
@RestControllerAdvice
public class BffExceptionHandler extends AbstractExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BffExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public BffExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Propagates downstream service error responses through the BFF.
     *
     * <p>Without this handler, a 4xx/5xx from a proxied service would surface as a generic 500.</p>
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<?> handleDownstreamError(RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        log.warn("Downstream service error: status={} body={}", status.value(), ex.getResponseBodyAsString());

        try {
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            return ResponseEntity.status(status).body(objectMapper.convertValue(body, Object.class));
        } catch (Exception parseError) {
            return buildErrorResponse(HttpStatus.valueOf(status.value()),
                    "DOWNSTREAM_ERROR", ex.getResponseBodyAsString(), null);
        }
    }
}
