package com.aegis.reporting.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link BalanceProjectionJpaEntity} entities.
 */
public interface BalanceProjectionJpaRepository extends JpaRepository<BalanceProjectionJpaEntity, UUID> {

    /**
     * Finds the balance projection for the given wallet.
     *
     * @param walletId the wallet identifier
     * @return an optional containing the entity if found, or empty otherwise
     */
    Optional<BalanceProjectionJpaEntity> findByWalletId(UUID walletId);
}
