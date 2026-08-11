package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class CurrencyMismatchException extends AegisException {

    public CurrencyMismatchException(String expected, String actual) {
        super("CURRENCY_MISMATCH", ErrorStatus.BAD_REQUEST,
                "Currency mismatch: expected " + expected + " but was " + actual);
    }
}
