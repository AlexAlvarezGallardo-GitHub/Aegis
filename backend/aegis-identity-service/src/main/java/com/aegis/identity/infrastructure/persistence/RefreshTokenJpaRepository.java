package com.aegis.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for refresh token entities.
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    /**
     * Finds a refresh token entity by its token hash.
     *
     * @param tokenHash the SHA-256 hash of the opaque refresh token
     * @return an optional containing the entity if found
     */
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);
}
