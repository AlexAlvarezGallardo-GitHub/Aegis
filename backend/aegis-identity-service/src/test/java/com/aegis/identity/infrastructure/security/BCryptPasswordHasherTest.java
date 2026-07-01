package com.aegis.identity.infrastructure.security;

import com.aegis.identity.domain.exception.WeakPasswordException;
import com.aegis.identity.domain.model.PasswordHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordHasherTest {

    private BCryptPasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new BCryptPasswordHasher();
    }

    @Test
    void shouldHashValidPassword() {
        PasswordHash hash = hasher.hash("SecureP@ss1");
        assertNotNull(hash);
        assertNotEquals("SecureP@ss1", hash.hash());
    }

    @Test
    void shouldMatchCorrectPassword() {
        PasswordHash hash = hasher.hash("SecureP@ss1");
        assertTrue(hasher.matches("SecureP@ss1", hash));
    }

    @Test
    void shouldNotMatchIncorrectPassword() {
        PasswordHash hash = hasher.hash("SecureP@ss1");
        assertFalse(hasher.matches("WrongP@ss1", hash));
    }

    @Test
    void shouldRejectPasswordTooShort() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> hasher.hash("Sh@1"));
        assertEquals("PASSWORD_TOO_SHORT", ex.getCode());
    }

    @Test
    void shouldRejectPasswordTooLong() {
        String longPassword = "A".repeat(129) + "a1@";
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> hasher.hash(longPassword));
        assertEquals("PASSWORD_TOO_LONG", ex.getCode());
    }

    @Test
    void shouldRejectPasswordMissingUppercase() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> hasher.hash("securep@ss1"));
        assertEquals("PASSWORD_MISSING_UPPERCASE", ex.getCode());
    }

    @Test
    void shouldRejectPasswordMissingLowercase() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> hasher.hash("SECUREP@SS1"));
        assertEquals("PASSWORD_MISSING_LOWERCASE", ex.getCode());
    }

    @Test
    void shouldRejectPasswordMissingDigit() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> hasher.hash("SecureP@ss"));
        assertEquals("PASSWORD_MISSING_DIGIT", ex.getCode());
    }

    @Test
    void shouldRejectPasswordMissingSpecialCharacter() {
        WeakPasswordException ex = assertThrows(WeakPasswordException.class,
                () -> hasher.hash("SecurePass1"));
        assertEquals("PASSWORD_MISSING_SPECIAL_CHARACTER", ex.getCode());
    }
}
