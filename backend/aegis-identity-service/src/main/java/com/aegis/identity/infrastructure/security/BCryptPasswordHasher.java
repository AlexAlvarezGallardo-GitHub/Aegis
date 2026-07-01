package com.aegis.identity.infrastructure.security;

import com.aegis.identity.domain.exception.WeakPasswordException;
import com.aegis.identity.domain.model.PasswordHash;
import com.aegis.identity.domain.port.outbound.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    @Override
    public PasswordHash hash(String rawPassword) {
        validate(rawPassword);
        return PasswordHash.of(ENCODER.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        return ENCODER.matches(rawPassword, hash.hash());
    }

    private void validate(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            throw new WeakPasswordException("PASSWORD_TOO_SHORT",
                    "Password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (password.length() > MAX_LENGTH) {
            throw new WeakPasswordException("PASSWORD_TOO_LONG",
                    "Password must not exceed " + MAX_LENGTH + " characters.");
        }
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            throw new WeakPasswordException("PASSWORD_MISSING_UPPERCASE",
                    "Password must contain at least one uppercase letter.");
        }
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            throw new WeakPasswordException("PASSWORD_MISSING_LOWERCASE",
                    "Password must contain at least one lowercase letter.");
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            throw new WeakPasswordException("PASSWORD_MISSING_DIGIT",
                    "Password must contain at least one digit.");
        }
        if (password.chars().allMatch(Character::isLetterOrDigit)) {
            throw new WeakPasswordException("PASSWORD_MISSING_SPECIAL_CHARACTER",
                    "Password must contain at least one special character.");
        }
    }
}
