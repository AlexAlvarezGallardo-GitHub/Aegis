package com.aegis.identity.domain.model;

import com.aegis.identity.domain.exception.InvalidEmailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        Email email = Email.of("user@example.com");
        assertEquals("user@example.com", email.value());
    }

    @Test
    void shouldNormalizeEmailToLowerCase() {
        Email email = Email.of("User@Example.COM");
        assertEquals("user@example.com", email.value());
    }

    @Test
    void shouldTrimWhitespace() {
        Email email = Email.of("  user@example.com  ");
        assertEquals("user@example.com", email.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "missing@", "@domain.com", "user@", "", "user@.com"})
    void shouldRejectInvalidEmailFormats(String invalidEmail) {
        assertThrows(InvalidEmailException.class, () -> Email.of(invalidEmail));
    }

    @Test
    void shouldRejectNullEmail() {
        assertThrows(NullPointerException.class, () -> Email.of(null));
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(InvalidEmailException.class, () -> Email.of("   "));
    }

    @Test
    void shouldRejectEmailExceedingMaxLength() {
        String longEmail = "a".repeat(250) + "@b.com";
        assertThrows(InvalidEmailException.class, () -> Email.of(longEmail));
    }

    @Test
    void shouldBeEqualForSameNormalizedEmail() {
        Email email1 = Email.of("User@Example.com");
        Email email2 = Email.of("user@example.com");
        assertEquals(email1, email2);
    }
}
