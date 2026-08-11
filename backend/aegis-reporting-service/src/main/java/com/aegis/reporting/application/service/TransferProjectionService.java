package com.aegis.reporting.application.service;

import com.aegis.reporting.domain.model.TransferProjection;
import com.aegis.reporting.domain.port.outbound.TransferProjectionRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Application service that coordinates the persistence and querying of transfer projections.
 */
@Service
public class TransferProjectionService {

    private final TransferProjectionRepository transferProjectionRepository;

    public TransferProjectionService(TransferProjectionRepository transferProjectionRepository) {
        this.transferProjectionRepository = transferProjectionRepository;
    }

    /**
     * Persists the given transfer projection.
     *
     * @param projection the transfer projection to save
     * @return the saved transfer projection
     */
    public TransferProjection save(TransferProjection projection) {
        return transferProjectionRepository.save(projection);
    }

    /**
     * Finds the transfer projection for the given transfer.
     *
     * @param transferId the transfer identifier
     * @return an optional containing the projection if found, or empty otherwise
     */
    public Optional<TransferProjection> findByTransferId(UUID transferId) {
        return transferProjectionRepository.findByTransferId(transferId);
    }
}
