package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;

import java.util.UUID;

public class WalletNotFoundException extends AegisException {

    public WalletNotFoundException(UUID walletId) {
        super("WALLET_NOT_FOUND", "Wallet not found: " + walletId);
    }
}
