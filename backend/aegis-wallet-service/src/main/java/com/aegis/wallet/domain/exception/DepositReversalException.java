package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

/**
 * Thrown when a deposit reversal cannot be applied (entry not found, already
 * reversed, or amount mismatch).
 */
public class DepositReversalException extends AegisException {

    private static final String CODE = "DEPOSIT_REVERSAL";

    public DepositReversalException(String message) {
        super(CODE, ErrorStatus.CONFLICT, message);
    }
}
