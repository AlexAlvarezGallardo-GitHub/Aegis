package com.aegis.identity.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Password value object")
class PasswordTest {

    @Test
    @DisplayName("Should accept a valid password meeting all rules")
    void shouldAcceptValidPassword() {
        assertDoesNotThrow(() -> Password.of("SecureP@ss1"));
    }

    @Test
    @DisplayName("Should reject null password")
    void shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> Password.of(null));
    }

    @Nested
    @DisplayName("Strength validation")
    class StrengthValidation {

        @Test
        @DisplayName("Should reject password shorter than 8 characters")
        void shouldRejectTooShort() {
            var ex = assertThrows(
                    com.aegis.identity.domain.exception.WeakPasswordException.class,
                    () -> Password.of("Sh@1"));
            assertEquals("PASSWORD_TOO_SHORT", ex.getCode());
        }

        @Test
        @DisplayName("Should reject password longer than 128 characters")
        void shouldRejectTooLong() {
            String longPassword = "A".repeat(126) + "a1@";
            var ex = assertThrows(
                    com.aegis.identity.domain.exception.WeakPasswordException.class,
                    () -> Password.of(longPassword));
            assertEquals("PASSWORD_TOO_LONG", ex.getCode());
        }

        @Test
        @DisplayName("Should reject password without uppercase letter")
        void shouldRejectMissingUppercase() {
            var ex = assertThrows(
                    com.aegis.identity.domain.exception.WeakPasswordException.class,
                    () -> Password.of("securep@ss1"));
            assertEquals("PASSWORD_MISSING_UPPERCASE", ex.getCode());
        }

        @Test
        @DisplayName("Should reject password without lowercase letter")
        void shouldRejectMissingLowercase() {
            var ex = assertThrows(
                    com.aegis.identity.domain.exception.WeakPasswordException.class,
                    () -> Password.of("SECUREP@SS1"));
            assertEquals("PASSWORD_MISSING_LOWERCASE", ex.getCode());
        }

        @Test
        @DisplayName("Should reject password without digit")
        void shouldRejectMissingDigit() {
            var ex = assertThrows(
                    com.aegis.identity.domain.exception.WeakPasswordException.class,
                    () -> Password.of("SecureP@ss"));
            assertEquals("PASSWORD_MISSING_DIGIT", ex.getCode());
        }

        @Test
        @DisplayName("Should reject password without special character")
        void shouldRejectMissingSpecialCharacter() {
            var ex = assertThrows(
                    com.aegis.identity.domain.exception.WeakPasswordException.class,
                    () -> Password.of("SecurePass1"));
            assertEquals("PASSWORD_MISSING_SPECIAL_CHARACTER", ex.getCode());
        }
    }

    @Test
    @DisplayName("toString should not expose the password value")
    void toStringShouldNotExposeValue() {
        Password password = Password.of("SecureP@ss1");
        assertEquals("[PROTECTED]", password.toString());
    }
}
