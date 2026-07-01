package com.aegis.identity.domain.model;

import com.aegis.identity.domain.exception.InvalidEmailException;

import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private static final int MAX_LENGTH = 255;

    public Email {
        Objects.requireNonNull(value, "Email must not be null");
        String normalized = value.trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new InvalidEmailException("Email must not be blank");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new InvalidEmailException("Email must not exceed " + MAX_LENGTH + " characters");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new InvalidEmailException("Invalid email format: " + normalized);
        }
        value = normalized;
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
