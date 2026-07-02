package com.aegis.identity.domain.model;

public record Credentials(Email email, String password) {
    public Credentials {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
    }
}
