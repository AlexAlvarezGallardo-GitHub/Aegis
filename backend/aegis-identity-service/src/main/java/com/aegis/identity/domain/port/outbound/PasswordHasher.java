package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.model.PasswordHash;

/**
 * Outbound port for hashing and verifying user passwords.
 */
public interface PasswordHasher {

    /**
     * Hashes the given raw password.
     *
     * @param rawPassword the plain-text password
     * @return the hashed password value object
     */
    PasswordHash hash(String rawPassword);

    /**
     * Verifies whether the given raw password matches the stored hash.
     *
     * @param rawPassword the plain-text password
     * @param hash        the stored password hash
     * @return true if the password matches the hash, false otherwise
     */
    boolean matches(String rawPassword, PasswordHash hash);
}
