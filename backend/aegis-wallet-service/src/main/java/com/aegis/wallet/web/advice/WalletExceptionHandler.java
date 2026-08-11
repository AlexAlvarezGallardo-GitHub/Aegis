package com.aegis.wallet.web.advice;

import com.aegis.common.web.advice.AbstractExceptionHandler;
import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.DuplicateDepositException;
import com.aegis.wallet.domain.exception.HoldNotActiveException;
import com.aegis.wallet.domain.exception.HoldNotFoundException;
import com.aegis.wallet.domain.exception.InsufficientFundsException;
import com.aegis.wallet.domain.exception.WalletLimitExceededException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.exception.WalletOperationNotAllowedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class WalletExceptionHandler extends AbstractExceptionHandler {

    @ExceptionHandler(WalletLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleWalletLimitExceeded(WalletLimitExceededException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(HoldNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleHoldNotFound(HoldNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(DuplicateDepositException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateDeposit(DuplicateDepositException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(WalletOperationNotAllowedException.class)
    public ResponseEntity<Map<String, Object>> handleWalletOperationNotAllowed(WalletOperationNotAllowedException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(HoldNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleHoldNotActive(HoldNotActiveException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientFunds(InsufficientFundsException ex) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(WalletNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleWalletNotActive(WalletNotActiveException ex) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(CurrencyMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleCurrencyMismatch(CurrencyMismatchException ex) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), null);
    }
}
