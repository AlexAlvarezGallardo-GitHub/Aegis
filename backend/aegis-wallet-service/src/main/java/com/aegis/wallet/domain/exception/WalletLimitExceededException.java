package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;

public class WalletLimitExceededException extends AegisException {

    public WalletLimitExceededException(int maxWallets) {
        super("WALLET_LIMIT_EXCEEDED", "User cannot have more than " + maxWallets + " wallets");
    }
}
