package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class InvalidCredentialsException extends AegisException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Invalid email or password.");
    }
}
