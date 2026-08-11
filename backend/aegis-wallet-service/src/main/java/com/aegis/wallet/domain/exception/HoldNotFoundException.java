package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

public class HoldNotFoundException extends AegisException {

    public HoldNotFoundException(UUID holdId) {
        super("HOLD_NOT_FOUND", ErrorStatus.NOT_FOUND, "Hold not found: " + holdId);
    }
}
