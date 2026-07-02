package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class AccountLockedException extends AegisException {

    public AccountLockedException() {
        super("ACCOUNT_LOCKED", "Account is locked due to too many failed login attempts. Please contact support.");
    }
}
