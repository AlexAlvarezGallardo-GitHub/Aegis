package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;
import com.aegis.payment.domain.model.TransferStatus;

/**
 * Thrown when a transfer state transition violates the state machine rules.
 */
public class InvalidTransferStateException extends AegisException {

    private static final String CODE = "INVALID_TRANSFER_STATE";

    /**
     * Creates a new invalid-state exception.
     *
     * @param current   the current transfer status
     * @param intended  the intended target status
     */
    public InvalidTransferStateException(TransferStatus current, TransferStatus intended) {
        super(CODE, ErrorStatus.BAD_REQUEST,
                "Invalid state transition from " + current + " to " + intended);
    }
}
