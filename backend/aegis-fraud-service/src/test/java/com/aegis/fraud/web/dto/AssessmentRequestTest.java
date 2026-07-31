package com.aegis.fraud.web.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssessmentRequest - Validation")
class AssessmentRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Nested
    @DisplayName("When validating a valid request")
    class WhenValidatingValidRequest {

        @Test
        @DisplayName("Should pass validation with all required fields")
        void shouldPassValidationWithAllRequiredFields() {
            AssessmentRequest request = new AssessmentRequest(
                    UUID.randomUUID(), "TRANSFER", new BigDecimal("1500.50"), "EUR",
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ES");

            Set<ConstraintViolation<AssessmentRequest>> violations = validator.validate(request);

            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("Should pass validation with null optional fields")
        void shouldPassValidationWithNullOptionalFields() {
            AssessmentRequest request = new AssessmentRequest(
                    UUID.randomUUID(), "TRANSFER", new BigDecimal("100.00"), "EUR",
                    null, null, UUID.randomUUID(), null);

            Set<ConstraintViolation<AssessmentRequest>> violations = validator.validate(request);

            assertTrue(violations.isEmpty());
        }
    }

    @Nested
    @DisplayName("When validating an invalid request")
    class WhenValidatingInvalidRequest {

        @Test
        @DisplayName("Should fail when transactionId is null")
        void shouldFailWhenTransactionIdIsNull() {
            AssessmentRequest request = new AssessmentRequest(
                    null, "TRANSFER", BigDecimal.TEN, "EUR",
                    null, null, UUID.randomUUID(), null);

            Set<ConstraintViolation<AssessmentRequest>> violations = validator.validate(request);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("transactionId")));
        }

        @Test
        @DisplayName("Should fail when transactionType is blank")
        void shouldFailWhenTransactionTypeIsBlank() {
            AssessmentRequest request = new AssessmentRequest(
                    UUID.randomUUID(), "", BigDecimal.TEN, "EUR",
                    null, null, UUID.randomUUID(), null);

            Set<ConstraintViolation<AssessmentRequest>> violations = validator.validate(request);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("transactionType")));
        }

        @Test
        @DisplayName("Should fail when amount is null")
        void shouldFailWhenAmountIsNull() {
            AssessmentRequest request = new AssessmentRequest(
                    UUID.randomUUID(), "TRANSFER", null, "EUR",
                    null, null, UUID.randomUUID(), null);

            Set<ConstraintViolation<AssessmentRequest>> violations = validator.validate(request);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
        }

        @Test
        @DisplayName("Should fail when amount is negative")
        void shouldFailWhenAmountIsNegative() {
            AssessmentRequest request = new AssessmentRequest(
                    UUID.randomUUID(), "TRANSFER", new BigDecimal("-100.00"), "EUR",
                    null, null, UUID.randomUUID(), null);

            Set<ConstraintViolation<AssessmentRequest>> violations = validator.validate(request);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
        }

        @Test
        @DisplayName("Should fail when userId is null")
        void shouldFailWhenUserIdIsNull() {
            AssessmentRequest request = new AssessmentRequest(
                    UUID.randomUUID(), "TRANSFER", BigDecimal.TEN, "EUR",
                    null, null, null, null);

            Set<ConstraintViolation<AssessmentRequest>> violations = validator.validate(request);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("userId")));
        }

        @Test
        @DisplayName("Should fail when currency is blank")
        void shouldFailWhenCurrencyIsBlank() {
            AssessmentRequest request = new AssessmentRequest(
                    UUID.randomUUID(), "TRANSFER", BigDecimal.TEN, "",
                    null, null, UUID.randomUUID(), null);

            Set<ConstraintViolation<AssessmentRequest>> violations = validator.validate(request);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("currency")));
        }
    }
}
