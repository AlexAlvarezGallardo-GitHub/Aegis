package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class WalletOperationNotAllowedException extends AegisException {

    public WalletOperationNotAllowedException(String message) {
        super("WALLET_OPERATION_NOT_ALLOWED", message);
    }
}
