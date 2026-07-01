package com.aegis.identity.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class WeakPasswordException extends AegisException {

    public WeakPasswordException(String code, String message) {
        super(code, message);
    }
}
