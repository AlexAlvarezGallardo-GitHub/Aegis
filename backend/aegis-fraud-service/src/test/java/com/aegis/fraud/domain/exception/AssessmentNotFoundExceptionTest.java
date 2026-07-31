package com.aegis.fraud.domain.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssessmentNotFoundException - Domain Exception")
class AssessmentNotFoundExceptionTest {

    @Nested
    @DisplayName("When creating exception")
    class WhenCreatingException {

        @Test
        @DisplayName("Should set correct error code")
        void shouldSetCorrectErrorCode() {
            UUID assessmentId = UUID.randomUUID();
            AssessmentNotFoundException exception = new AssessmentNotFoundException(assessmentId);

            assertEquals("ASSESSMENT_NOT_FOUND", exception.getCode());
        }

        @Test
        @DisplayName("Should include assessment ID in message")
        void shouldIncludeAssessmentIdInMessage() {
            UUID assessmentId = UUID.randomUUID();
            AssessmentNotFoundException exception = new AssessmentNotFoundException(assessmentId);

            assertTrue(exception.getMessage().contains(assessmentId.toString()));
            assertTrue(exception.getMessage().contains("Fraud assessment not found"));
        }

        @Test
        @DisplayName("Should have unique error ID")
        void shouldHaveUniqueErrorId() {
            UUID assessmentId = UUID.randomUUID();
            AssessmentNotFoundException exception1 = new AssessmentNotFoundException(assessmentId);
            AssessmentNotFoundException exception2 = new AssessmentNotFoundException(assessmentId);

            assertNotNull(exception1.getErrorId());
            assertNotNull(exception2.getErrorId());
            assertNotEquals(exception1.getErrorId(), exception2.getErrorId());
        }

        @Test
        @DisplayName("Should have timestamp")
        void shouldHaveTimestamp() {
            UUID assessmentId = UUID.randomUUID();
            AssessmentNotFoundException exception = new AssessmentNotFoundException(assessmentId);

            assertNotNull(exception.getTimestamp());
        }

        @Test
        @DisplayName("Should be a RuntimeException")
        void shouldBeRuntimeException() {
            AssessmentNotFoundException exception = new AssessmentNotFoundException(UUID.randomUUID());
            assertTrue(exception instanceof RuntimeException);
        }
    }
}
