package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;

/**
 * Thrown when a wallet operation would result in a negative balance.
 */
public class InsufficientFundsException extends AegisException {

    public InsufficientFundsException(String message) {
        super("INSUFFICIENT_FUNDS", message);
    }
}
