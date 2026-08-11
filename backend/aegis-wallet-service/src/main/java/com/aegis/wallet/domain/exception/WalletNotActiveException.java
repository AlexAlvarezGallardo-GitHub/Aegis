package com.aegis.wallet.domain.exception;

import com.aegis.common.domain.exception.AegisException;
import com.aegis.common.domain.exception.ErrorStatus;

import java.util.UUID;

public class WalletNotActiveException extends AegisException {

    public WalletNotActiveException(UUID walletId, String status) {
        super("WALLET_NOT_ACTIVE", ErrorStatus.CONFLICT,
                "Wallet " + walletId + " is not active (status=" + status + ")");
    }
}
