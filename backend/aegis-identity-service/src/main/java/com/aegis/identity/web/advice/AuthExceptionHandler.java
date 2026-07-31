package com.aegis.identity.web.advice;

import com.aegis.identity.domain.exception.AccountLockedException;
import com.aegis.identity.domain.exception.AccountSuspendedException;
import com.aegis.identity.domain.exception.InvalidCredentialsException;
import com.aegis.common.web.advice.AbstractExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AuthExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(AccountLockedException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountSuspended(AccountSuspendedException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getCode(), ex.getMessage(), null);
    }
}
