package com.aegis.bff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Propagates downstream service error responses through the BFF.
 * <p>
 * Without this handler, a 4xx/5xx from a proxied service would surface as a generic 500.
 * </p>
 */
@RestControllerAdvice
public class BffExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BffExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public BffExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Object> handleDownstreamError(RestClientResponseException ex) {
        HttpStatusCode status = ex.getStatusCode();
        log.warn("Downstream service error: status={} body={}", status.value(), ex.getResponseBodyAsString());

        try {
            JsonNode body = objectMapper.readTree(ex.getResponseBodyAsString());
            return ResponseEntity.status(status).body(objectMapper.convertValue(body, Object.class));
        } catch (Exception parseError) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("code", "DOWNSTREAM_ERROR");
            fallback.put("message", ex.getResponseBodyAsString());
            fallback.put("details", null);
            fallback.put("timestamp", java.time.Instant.now().toString());
            return ResponseEntity.status(status).body(fallback);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("BFF unexpected error", ex);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "INTERNAL_ERROR");
        body.put("message", "An unexpected error occurred.");
        body.put("details", null);
        body.put("timestamp", java.time.Instant.now().toString());
        return ResponseEntity.status(500).body(body);
    }
}
