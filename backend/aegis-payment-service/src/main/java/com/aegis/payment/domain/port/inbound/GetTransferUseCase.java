package com.aegis.payment.domain.port.inbound;

import com.aegis.payment.domain.model.Transfer;

import java.util.UUID;

/**
 * Port for retrieving a transfer by its identifier.
 */
public interface GetTransferUseCase {

    /**
     * Finds a transfer by its identifier.
     *
     * @param transferId the transfer identifier
     * @return the transfer
     * @throws com.aegis.payment.domain.exception.TransferNotFoundException if not found
     */
    Transfer findById(UUID transferId);
}
