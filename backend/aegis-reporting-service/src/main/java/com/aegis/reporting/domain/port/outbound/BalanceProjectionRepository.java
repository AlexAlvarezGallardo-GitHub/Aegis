package com.aegis.reporting.domain.port.outbound;

import com.aegis.reporting.domain.model.BalanceProjection;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and querying balance projections.
 */
public interface BalanceProjectionRepository {

    /**
     * Persists the given balance projection.
     *
     * @param projection the balance projection to save
     * @return the saved balance projection
     */
    BalanceProjection save(BalanceProjection projection);

    /**
     * Finds the balance projection for the given wallet.
     *
     * @param walletId the wallet identifier
     * @return an optional containing the projection if found, or empty otherwise
     */
    Optional<BalanceProjection> findByWalletId(UUID walletId);
}
