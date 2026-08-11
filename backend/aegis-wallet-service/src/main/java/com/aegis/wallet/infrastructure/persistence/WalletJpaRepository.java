package com.aegis.wallet.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for wallet entities.
 */
public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, UUID> {

    List<WalletJpaEntity> findByUserId(UUID userId);

    long countByUserId(UUID userId);

    /**
     * Finds a wallet row with a pessimistic write lock (SELECT FOR UPDATE).
     *
     * @param id the wallet id
     * @return the locked entity if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletJpaEntity w WHERE w.id = :id")
    Optional<WalletJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
