package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class AccountSuspendedException extends AegisException {

    public AccountSuspendedException() {
        super("ACCOUNT_SUSPENDED", "Account has been suspended. Please contact support.");
    }
}
