package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class WeakPasswordException extends AegisException {

    public WeakPasswordException(String code, String message) {
        super(code, ErrorStatus.BAD_REQUEST, message);
    }
}
