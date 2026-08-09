package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class AccountSuspendedException extends AegisException {

    public AccountSuspendedException() {
        super("ACCOUNT_SUSPENDED", ErrorStatus.UNAUTHORIZED, "Account has been suspended. Please contact support.");
    }
}
