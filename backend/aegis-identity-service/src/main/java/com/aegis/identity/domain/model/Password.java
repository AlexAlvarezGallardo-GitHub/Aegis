package com.aegis.identity.domain.model;

import com.aegis.identity.domain.exception.WeakPasswordException;

import java.util.Objects;

/**
 * Value object representing a validated user password.
 *
 * <p>Enforces the following rules on construction:</p>
 * <ul>
 *   <li>Length between 8 and 128 characters</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one special (non-letter, non-digit) character</li>
 * </ul>
 *
 * @param value the validated plain-text password
 */
public record Password(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    public Password {
        Objects.requireNonNull(value, "Password must not be null");
        validate(value);
    }

    /**
     * Creates a new {@code Password} after validating strength rules.
     *
     * @param rawPassword the plain-text password to validate
     * @return a validated Password instance
     * @throws WeakPasswordException if the password does not meet strength requirements
     */
    public static Password of(String rawPassword) {
        return new Password(rawPassword);
    }

    private static void validate(String password) {
        if (password.length() < MIN_LENGTH) {
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

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
