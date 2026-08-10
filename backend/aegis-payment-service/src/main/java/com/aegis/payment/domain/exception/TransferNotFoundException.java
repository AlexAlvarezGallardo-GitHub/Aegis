package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when a transfer cannot be found by its identifier.
 */
public class TransferNotFoundException extends AegisException {

    private static final String CODE = "TRANSFER_NOT_FOUND";

    /**
     * Creates a new exception for the given transfer identifier.
     *
     * @param transferId the transfer identifier that could not be found
     */
    public TransferNotFoundException(UUID transferId) {
        super(CODE, ErrorStatus.NOT_FOUND, "Transfer not found: " + transferId);
    }
}
