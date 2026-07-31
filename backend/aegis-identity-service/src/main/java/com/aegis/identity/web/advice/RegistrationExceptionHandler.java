package com.aegis.identity.web.advice;

import com.aegis.common.web.advice.AbstractExceptionHandler;
import com.aegis.identity.domain.exception.DuplicateEmailException;
import com.aegis.identity.domain.exception.InvalidEmailException;
import com.aegis.identity.domain.exception.WeakPasswordException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class RegistrationExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED",
                "A user with the given email already exists.", null);
    }

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidEmail(InvalidEmailException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(WeakPasswordException.class)
    public ResponseEntity<Map<String, Object>> handleWeakPassword(WeakPasswordException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage(), null);
    }
}
