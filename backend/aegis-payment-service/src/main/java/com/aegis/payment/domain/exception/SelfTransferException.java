package com.aegis.payment.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

/**
 * Thrown when a transfer attempts to move funds between the same wallet.
 */
public class SelfTransferException extends AegisException {

    private static final String CODE = "SELF_TRANSFER";

    /**
     * Creates a new self-transfer exception.
     *
     * @param walletId the wallet that was used as both source and destination
     */
    public SelfTransferException(UUID walletId) {
        super(CODE, ErrorStatus.BAD_REQUEST,
                "Source and destination wallets must differ: " + walletId);
    }
}
