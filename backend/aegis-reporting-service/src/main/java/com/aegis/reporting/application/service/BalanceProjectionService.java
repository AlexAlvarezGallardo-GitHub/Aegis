package com.aegis.reporting.application.service;

import com.aegis.reporting.domain.model.BalanceProjection;
import com.aegis.reporting.domain.port.outbound.BalanceProjectionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Application service that coordinates the persistence and querying of balance projections.
 */
@Service
public class BalanceProjectionService {

    private final BalanceProjectionRepository balanceProjectionRepository;

    public BalanceProjectionService(BalanceProjectionRepository balanceProjectionRepository) {
        this.balanceProjectionRepository = balanceProjectionRepository;
    }

    /**
     * Persists the given balance projection.
     *
     * @param projection the balance projection to save
     * @return the saved balance projection
     */
    public BalanceProjection save(BalanceProjection projection) {
        return balanceProjectionRepository.save(projection);
    }

    /**
     * Finds the balance projection for the given wallet.
     *
     * @param walletId the wallet identifier
     * @return an optional containing the projection if found, or empty otherwise
     */
    public Optional<BalanceProjection> findByWalletId(UUID walletId) {
        return balanceProjectionRepository.findByWalletId(walletId);
    }
}
