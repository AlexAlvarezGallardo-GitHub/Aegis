package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class DuplicateEmailException extends AegisException {

    public DuplicateEmailException(String email) {
        super("EMAIL_ALREADY_REGISTERED", ErrorStatus.CONFLICT, "An account with this email address already exists.");
    }
}
