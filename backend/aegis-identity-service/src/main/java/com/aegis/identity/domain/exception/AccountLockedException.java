package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class AccountLockedException extends AegisException {

    public AccountLockedException() {
        super("ACCOUNT_LOCKED", ErrorStatus.UNAUTHORIZED,
                "Account is locked due to too many failed login attempts. Please contact support.");
    }
}
