package com.aegis.fraud.web.advice;

import com.aegis.fraud.domain.exception.AssessmentNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FraudExceptionHandler - Exception Handling")
class FraudExceptionHandlerTest {

    private FraudExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new FraudExceptionHandler();
    }

    @Nested
    @DisplayName("When handling AssessmentNotFoundException")
    class WhenHandlingAssessmentNotFoundException {

        @Test
        @DisplayName("Should return 404 with correct error code")
        void shouldReturn404WithCorrectErrorCode() {
            UUID assessmentId = UUID.randomUUID();
            AssessmentNotFoundException exception = new AssessmentNotFoundException(assessmentId);

            ResponseEntity<Map<String, Object>> response = handler.handleAssessmentNotFound(exception);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("ASSESSMENT_NOT_FOUND", response.getBody().get("code"));
            assertTrue(response.getBody().get("message").toString().contains(assessmentId.toString()));
            assertNotNull(response.getBody().get("timestamp"));
        }

        @Test
        @DisplayName("Should include error details in response body")
        void shouldIncludeErrorDetailsInResponseBody() {
            UUID assessmentId = UUID.randomUUID();
            AssessmentNotFoundException exception = new AssessmentNotFoundException(assessmentId);

            ResponseEntity<Map<String, Object>> response = handler.handleAssessmentNotFound(exception);

            assertNotNull(response.getBody());
            assertTrue(response.getBody().containsKey("code"));
            assertTrue(response.getBody().containsKey("message"));
            assertTrue(response.getBody().containsKey("timestamp"));
        }
    }

    @Nested
    @DisplayName("When handling inherited exceptions")
    class WhenHandlingInheritedExceptions {

        @Test
        @DisplayName("Should handle IllegalArgumentException with 400")
        void shouldHandleIllegalArgumentWith400() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid field");

            ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgument(exception);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("FIELD_REQUIRED", response.getBody().get("code"));
        }

        @Test
        @DisplayName("Should handle generic Exception with 500")
        void shouldHandleGenericExceptionWith500() {
            Exception exception = new RuntimeException("Unexpected error");

            ResponseEntity<Map<String, Object>> response = handler.handleGeneric(exception);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("INTERNAL_ERROR", response.getBody().get("code"));
        }
    }
}
