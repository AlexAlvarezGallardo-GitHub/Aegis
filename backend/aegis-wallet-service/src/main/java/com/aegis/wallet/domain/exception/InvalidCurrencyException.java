package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class InvalidCurrencyException extends AegisException {

    public InvalidCurrencyException(String currency) {
        super("INVALID_CURRENCY", "Invalid currency code: " + currency);
    }
}
