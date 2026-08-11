package com.aegis.reporting.domain.port.outbound;

import com.aegis.reporting.domain.model.TransferProjection;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for persisting and querying transfer projections.
 */
public interface TransferProjectionRepository {

    /**
     * Persists the given transfer projection.
     *
     * @param projection the transfer projection to save
     * @return the saved transfer projection
     */
    TransferProjection save(TransferProjection projection);

    /**
     * Finds the transfer projection for the given transfer.
     *
     * @param transferId the transfer identifier
     * @return an optional containing the projection if found, or empty otherwise
     */
    Optional<TransferProjection> findByTransferId(UUID transferId);
}
