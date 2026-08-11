package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when the fraud service rejects a transfer.
 */
public class FraudRejectedException extends AegisException {

    private static final String CODE = "TRANSFER_REJECTED_BY_FRAUD";

    /**
     * Creates a new fraud-rejected exception.
     *
     * @param transferId the transfer that was rejected
     */
    public FraudRejectedException(UUID transferId) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY,
                "Transfer rejected by fraud assessment: " + transferId);
    }
}
