package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class InvalidCurrencyException extends AegisException {

    public InvalidCurrencyException(String currency) {
        super("INVALID_CURRENCY", ErrorStatus.BAD_REQUEST, "Invalid currency code: " + currency);
    }
}
