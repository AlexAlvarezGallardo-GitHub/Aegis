package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class DuplicateDepositException extends AegisException {

    private static final String CODE = "DUPLICATE_DEPOSIT";

    public DuplicateDepositException(String reference) {
        super(CODE, ErrorStatus.CONFLICT, "Duplicate deposit request with reference: " + reference);
    }
}
