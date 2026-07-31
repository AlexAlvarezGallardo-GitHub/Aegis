package com.aegis.identity.infrastructure.security;

import com.aegis.identity.domain.model.PasswordHash;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BCryptPasswordHasher}.
 *
 * <p>Password strength validation is now the responsibility of the
 * {@link com.aegis.identity.domain.model.Password} value object.
 * These tests only verify hashing and matching behaviour.</p>
 */
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
    void shouldProduceDifferentHashesForSamePassword() {
        PasswordHash hash1 = hasher.hash("SecureP@ss1");
        PasswordHash hash2 = hasher.hash("SecureP@ss1");
        assertNotEquals(hash1.hash(), hash2.hash());
    }
}
