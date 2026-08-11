package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class HoldNotActiveException extends AegisException {

    public HoldNotActiveException(String message) {
        super("HOLD_NOT_ACTIVE", ErrorStatus.CONFLICT, message);
    }
}
