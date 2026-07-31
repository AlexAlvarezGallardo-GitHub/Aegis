package com.aegis.identity.domain.port.outbound;

import com.aegis.identity.domain.model.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for managing refresh token persistence and lifecycle.
 */
public interface RefreshTokenRepository {

    /**
     * Finds a stored refresh token by its hashed value.
     *
     * @param tokenHash the SHA-256 hash of the opaque refresh token
     * @return an optional containing the stored token data if found
     */
    Optional<StoredRefreshToken> findByTokenHash(String tokenHash);

    /**
     * Persists a new refresh token.
     *
     * @param token the stored refresh token data to save
     */
    void save(StoredRefreshToken token);

    /**
     * Marks the given refresh token as revoked.
     *
     * @param tokenHash the hash of the token to revoke
     */
    void revoke(String tokenHash);

    /**
     * Data object representing a persisted refresh token.
     *
     * @param id         the unique identifier
     * @param tokenHash  the SHA-256 hash of the opaque token
     * @param userId     the user this token belongs to
     * @param expiresAt  the instant when the token expires
     * @param revokedAt  the instant when the token was revoked, or null if still active
     * @param createdAt  the instant when the token was created
     */
    record StoredRefreshToken(UUID id, String tokenHash, UserId userId,
                               Instant expiresAt, Instant revokedAt, Instant createdAt) {
    }
}
