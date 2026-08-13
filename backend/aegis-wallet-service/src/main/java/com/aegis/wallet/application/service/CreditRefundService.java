package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.CreditRefundUseCase;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service that credits a wallet with a refund, creating a REFUND
 * ledger entry. Idempotent by reference (refundId).
 */
@Service
public class CreditRefundService implements CreditRefundUseCase {

    private final WalletRepository walletRepository;

    public CreditRefundService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public CreditResult credit(CreditCommand command) {
        Wallet wallet = walletRepository.findByIdForUpdate(WalletId.of(command.walletId()))
                .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(command.walletId(), wallet.getStatus().name());
        }
        if (!wallet.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new CurrencyMismatchException(command.currency(), wallet.getCurrency());
        }

        String refundRef = command.refundId().toString();
        wallet.creditForRefund(command.amount(), refundRef, "Refund");

        walletRepository.save(wallet);

        return new CreditResult(
                command.refundId(),
                wallet.getWalletId().value(),
                wallet.getBalance(),
                Instant.now()
        );
    }
}
