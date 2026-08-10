package com.aegis.identity.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Shared codec for opaque refresh tokens: random value generation and SHA-256 hashing.
 *
 * <p>Both login (issue a refresh token alongside the access token) and refresh
 * (rotation) paths persist only the hash, never the raw token.</p>
 */
final class RefreshTokenCodec {

    private RefreshTokenCodec() {
    }

    /**
     * Generates a new opaque refresh token value.
     *
     * @return a 64-character random hex value
     */
    static String generateOpaqueToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Computes the SHA-256 hash of the given token value.
     *
     * @param token the raw token value
     * @return the hexadecimal SHA-256 digest
     */
    static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
