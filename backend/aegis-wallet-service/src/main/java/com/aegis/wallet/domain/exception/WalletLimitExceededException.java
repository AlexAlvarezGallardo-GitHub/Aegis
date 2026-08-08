package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

public class WalletLimitExceededException extends AegisException {

    public WalletLimitExceededException(int maxWallets) {
        super("WALLET_LIMIT_EXCEEDED", ErrorStatus.CONFLICT, "User cannot have more than " + maxWallets + " wallets");
    }
}
