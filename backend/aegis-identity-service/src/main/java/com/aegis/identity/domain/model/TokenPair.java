package com.aegis.identity.domain.model;

public record TokenPair(String accessToken, String refreshToken) {
    public TokenPair {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token must not be blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be blank");
        }
    }
}
