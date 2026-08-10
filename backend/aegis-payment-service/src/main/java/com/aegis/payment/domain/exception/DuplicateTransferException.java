package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

/**
 * Thrown when a transfer with the same (sourceWalletId, reference) already exists.
 */
public class DuplicateTransferException extends AegisException {

    private static final String CODE = "TRANSFER_DUPLICATE";

    /**
     * Creates a new duplicate-transfer exception.
     *
     * @param reference the idempotency reference that collided
     */
    public DuplicateTransferException(String reference) {
        super(CODE, ErrorStatus.CONFLICT, "Transfer with reference already exists: " + reference);
    }
}
