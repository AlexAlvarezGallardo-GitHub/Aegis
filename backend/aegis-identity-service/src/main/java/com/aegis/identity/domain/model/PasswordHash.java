package com.aegis.identity.domain.model;

import java.util.Objects;

public record PasswordHash(String hash) {

    public PasswordHash {
        Objects.requireNonNull(hash, "PasswordHash must not be null");
        if (hash.isBlank()) {
            throw new IllegalArgumentException("PasswordHash must not be blank");
        }
    }

    public static PasswordHash of(String hash) {
        return new PasswordHash(hash);
    }

    @Override
    public String toString() {
        return "[PROTECTED]";
    }
}
