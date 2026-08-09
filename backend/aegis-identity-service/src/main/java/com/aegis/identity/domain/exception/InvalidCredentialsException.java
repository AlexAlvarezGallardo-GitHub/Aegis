package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class InvalidCredentialsException extends AegisException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", ErrorStatus.UNAUTHORIZED, "Invalid email or password.");
    }
}
