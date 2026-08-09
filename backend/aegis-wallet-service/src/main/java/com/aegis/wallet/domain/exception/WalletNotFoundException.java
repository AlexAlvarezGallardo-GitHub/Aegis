package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

public class WalletNotFoundException extends AegisException {

    public WalletNotFoundException(UUID walletId) {
        super("WALLET_NOT_FOUND", ErrorStatus.NOT_FOUND, "Wallet not found: " + walletId);
    }
}
