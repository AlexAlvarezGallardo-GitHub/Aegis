package com.aegis.payment.domain.port.outbound;

import com.aegis.payment.domain.model.Transfer;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for persisting and retrieving transfers.
 */
public interface TransferRepository {

    /**
     * Persists a transfer.
     *
     * @param transfer the transfer to save
     * @return the saved transfer
     */
    Transfer save(Transfer transfer);

    /**
     * Finds a transfer by its identifier.
     *
     * @param transferId the transfer identifier
     * @return the transfer, or empty if not found
     */
    Optional<Transfer> findById(UUID transferId);

    /**
     * Checks whether a transfer with the given source wallet and reference already exists.
     *
     * @param sourceWalletId the source wallet identifier
     * @param reference      the idempotency reference
     * @return {@code true} if a transfer already exists
     */
    boolean existsBySourceWalletIdAndReference(UUID sourceWalletId, String reference);
}
