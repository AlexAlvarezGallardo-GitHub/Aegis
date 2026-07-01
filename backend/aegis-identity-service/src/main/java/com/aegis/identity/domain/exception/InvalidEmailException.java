package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class InvalidEmailException extends AegisException {

    public InvalidEmailException(String message) {
        super("INVALID_EMAIL_FORMAT", message);
    }
}
