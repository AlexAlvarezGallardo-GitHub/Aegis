package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class DuplicateDepositException extends AegisException {

    private static final String CODE = "DUPLICATE_DEPOSIT";

    public DuplicateDepositException(String reference) {
        super(CODE, "Duplicate deposit request with reference: " + reference);
    }
}
