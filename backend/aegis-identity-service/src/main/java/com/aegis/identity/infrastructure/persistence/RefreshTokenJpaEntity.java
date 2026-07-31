package com.aegis.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a refresh token stored in the database.
 *
 * <p>Each refresh token is stored as a SHA-256 hash of the opaque token value
 * to prevent leakage in case of database compromise.</p>
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenJpaEntity {

    @Id
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(UUID id, String tokenHash, UUID userId,
                                  Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = null;
    }

    public UUID getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Marks this refresh token as revoked at the current instant.
     */
    public void revoke() {
        this.revokedAt = Instant.now();
    }

    /**
     * Returns whether this token has been revoked.
     *
     * @return true if the token has been revoked
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Returns whether this token has expired.
     *
     * @return true if the token has expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
