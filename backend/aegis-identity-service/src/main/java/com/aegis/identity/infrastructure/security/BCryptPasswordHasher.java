package com.aegis.identity.infrastructure.security;

import com.aegis.identity.domain.model.PasswordHash;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt-based password hasher.
 *
 * <p>This adapter only performs hashing and matching. Password strength validation
 * is the responsibility of the {@link com.aegis.identity.domain.model.Password} value object.</p>
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(10);

    @Override
    public PasswordHash hash(String rawPassword) {
        return PasswordHash.of(ENCODER.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        return ENCODER.matches(rawPassword, hash.hash());
    }
}
