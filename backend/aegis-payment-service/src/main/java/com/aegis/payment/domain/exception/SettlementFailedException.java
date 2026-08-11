package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when the wallet service rejects or fails a hold/settle/release step of
 * the transfer saga. Triggers saga compensation (hold release).
 */
public class SettlementFailedException extends AegisException {

    private static final String CODE = "SETTLEMENT_FAILED";

    /**
     * Creates a new settlement-failed exception.
     *
     * @param message the error detail
     */
    public SettlementFailedException(String message) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY, message);
    }

    /**
     * Creates a new settlement-failed exception with a custom error code
     * (e.g. the wallet service's INSUFFICIENT_FUNDS propagated through the saga).
     *
     * @param code    the error code to surface
     * @param message the error detail
     * @param cause   the underlying cause
     */
    public SettlementFailedException(String code, String message, Throwable cause) {
        super(code, ErrorStatus.UNPROCESSABLE_ENTITY, message, cause);
    }

    /**
     * Creates a new settlement-failed exception with a cause.
     *
     * @param message the error detail
     * @param cause   the underlying cause
     */
    public SettlementFailedException(String message, Throwable cause) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY, message, cause);
    }

    /**
     * Creates a new settlement-failed exception for a specific transfer.
     *
     * @param transferId the transfer whose settlement failed
     * @param message    the error detail
     */
    public SettlementFailedException(UUID transferId, String message) {
        super(CODE, ErrorStatus.UNPROCESSABLE_ENTITY,
                "Settlement failed for transfer " + transferId + ": " + message);
    }
}
