package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.HoldNotActiveException;
import com.aegis.wallet.domain.exception.HoldNotFoundException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.HoldStatus;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.DebitHoldUseCase;
import com.aegis.wallet.domain.port.outbound.HoldRepository;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service that settles a hold into a debit-only ledger entry (PAYMENT).
 *
 * <p>This is the payment-specific settlement path — distinct from
 * {@link SettleTransferService} which moves funds between two wallets.</p>
 */
@Service
public class DebitHoldService implements DebitHoldUseCase {

    private final WalletRepository walletRepository;
    private final HoldRepository holdRepository;

    public DebitHoldService(WalletRepository walletRepository, HoldRepository holdRepository) {
        this.walletRepository = walletRepository;
        this.holdRepository = holdRepository;
    }

    @Override
    @Transactional
    public DebitResult debit(DebitCommand command) {
        Hold hold = holdRepository.findById(command.holdId())
                .orElseThrow(() -> new HoldNotFoundException(command.holdId()));

        // Idempotent: if already SETTLED for the same payment, return original result
        if (hold.getStatus() == HoldStatus.SETTLED
                && hold.getReference().equals(command.paymentId().toString())) {
            Wallet wallet = walletRepository.findById(WalletId.of(command.walletId()))
                    .orElseThrow(() -> new WalletNotFoundException(command.walletId()));
            return new DebitResult(
                    command.paymentId(), hold.getId(),
                    wallet.getWalletId().value(), wallet.getBalance(),
                    Instant.now()
            );
        }

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new HoldNotActiveException(
                    "Hold " + hold.getId() + " is " + hold.getStatus() + "; cannot debit");
        }

        if (!hold.getReference().equals(command.paymentId().toString())) {
            throw new IllegalArgumentException(
                    "Hold reference " + hold.getReference()
                            + " does not match paymentId " + command.paymentId());
        }
        if (hold.getAmount().compareTo(command.amount()) != 0) {
            throw new IllegalArgumentException(
                    "Hold amount " + hold.getAmount()
                            + " does not match payment amount " + command.amount());
        }
        if (!hold.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new CurrencyMismatchException(hold.getCurrency(), command.currency());
        }

        Wallet wallet = walletRepository.findByIdForUpdate(WalletId.of(command.walletId()))
                .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(command.walletId(), wallet.getStatus().name());
        }
        if (!wallet.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new CurrencyMismatchException(command.currency(), wallet.getCurrency());
        }

        String paymentRef = command.paymentId().toString();
        hold.settle();
        wallet.debitForPayment(command.amount(), paymentRef, "Payment");

        holdRepository.save(hold);
        walletRepository.save(wallet);

        return new DebitResult(
                command.paymentId(),
                hold.getId(),
                wallet.getWalletId().value(),
                wallet.getBalance(),
                Instant.now()
        );
    }
}
