package com.aegis.reporting.infrastructure.persistence;

import com.aegis.reporting.domain.model.BalanceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link BalanceProjection} entities.
 */
@Repository
public interface BalanceProjectionRepository extends JpaRepository<BalanceProjection, UUID> {

    /**
     * Finds the balance projection for the given wallet.
     *
     * @param walletId the wallet identifier
     * @return an optional containing the projection, or empty if not found
     */
    Optional<BalanceProjection> findByWalletId(UUID walletId);
}
