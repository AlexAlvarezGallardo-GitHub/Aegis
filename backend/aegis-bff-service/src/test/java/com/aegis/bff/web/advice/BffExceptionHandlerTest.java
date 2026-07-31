package com.aegis.bff.web.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BffExceptionHandler")
class BffExceptionHandlerTest {

    private BffExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BffExceptionHandler(new ObjectMapper());
    }

    @Nested
    @DisplayName("When downstream error has valid JSON body")
    class WhenDownstreamErrorHasJsonBody {

        @Test
        @DisplayName("Should propagate status and parsed JSON body")
        void shouldPropagateJsonBody() {
            // Arrange
            String jsonBody = """
                    {"code":"NOT_FOUND","message":"Wallet not found","timestamp":"2024-01-01T00:00:00Z"}
                    """;
            RestClientResponseException ex = createException(HttpStatus.NOT_FOUND, jsonBody);

            // Act
            ResponseEntity<?> response = handler.handleDownstreamError(ex);

            // Assert
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals("NOT_FOUND", body.get("code"));
            assertEquals("Wallet not found", body.get("message"));
        }
    }

    @Nested
    @DisplayName("When downstream error has non-JSON body")
    class WhenDownstreamErrorHasNonJsonBody {

        @Test
        @DisplayName("Should return DOWNSTREAM_ERROR envelope with raw body as message")
        void shouldReturnErrorEnvelope() {
            // Arrange
            String rawBody = "Internal Server Error - HTML page";
            RestClientResponseException ex = createException(HttpStatus.INTERNAL_SERVER_ERROR, rawBody);

            // Act
            ResponseEntity<?> response = handler.handleDownstreamError(ex);

            // Assert
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof Map);
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            assertEquals("DOWNSTREAM_ERROR", body.get("code"));
            assertEquals(rawBody, body.get("message"));
        }
    }

    @Nested
    @DisplayName("When downstream error has 400 status")
    class WhenDownstreamErrorHas400Status {

        @Test
        @DisplayName("Should propagate 400 status with JSON body")
        void shouldPropagate400Status() {
            // Arrange
            String jsonBody = """
                    {"code":"VALIDATION_ERROR","message":"Invalid currency"}
                    """;
            RestClientResponseException ex = createException(HttpStatus.BAD_REQUEST, jsonBody);

            // Act
            ResponseEntity<?> response = handler.handleDownstreamError(ex);

            // Assert
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    /**
     * Helper to create a RestClientResponseException with a given status and body.
     */
    private RestClientResponseException createException(HttpStatus status, String body) {
        return new RestClientResponseException(
                "Downstream error",
                status.value(),
                status.getReasonPhrase(),
                new HttpHeaders(),
                body.getBytes(StandardCharsets.UTF_8),
                null
        ) {};
    }
}
