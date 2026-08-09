package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class InvalidEmailException extends AegisException {

    public InvalidEmailException(String message) {
        super("INVALID_EMAIL_FORMAT", ErrorStatus.BAD_REQUEST, message);
    }
}
