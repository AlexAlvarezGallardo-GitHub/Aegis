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
import com.aegis.wallet.domain.port.inbound.SettleTransferUseCase;
import com.aegis.wallet.domain.port.outbound.HoldRepository;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SettleTransferService implements SettleTransferUseCase {

    private final WalletRepository walletRepository;
    private final HoldRepository holdRepository;

    public SettleTransferService(WalletRepository walletRepository, HoldRepository holdRepository) {
        this.walletRepository = walletRepository;
        this.holdRepository = holdRepository;
    }

    @Override
    @Transactional
    public SettleResult settle(SettleCommand command) {
        Hold hold = holdRepository.findById(command.holdId())
                .orElseThrow(() -> new HoldNotFoundException(command.holdId()));

        // Idempotent: if already SETTLED for the same transfer, return original result
        // without duplicating ledger entries.
        if (hold.getStatus() == HoldStatus.SETTLED
                && hold.getReference().equals(command.transferId().toString())) {
            Wallet source = walletRepository.findById(WalletId.of(command.sourceWalletId()))
                    .orElseThrow(() -> new WalletNotFoundException(command.sourceWalletId()));
            Wallet dest = walletRepository.findById(WalletId.of(command.destWalletId()))
                    .orElseThrow(() -> new WalletNotFoundException(command.destWalletId()));
            return new SettleResult(
                    command.transferId(), hold.getId(),
                    source.getWalletId().value(), source.getBalance(),
                    dest.getWalletId().value(), dest.getBalance(),
                    Instant.now()
            );
        }

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new HoldNotActiveException(
                    "Hold " + hold.getId() + " is " + hold.getStatus() + "; cannot settle");
        }

        if (!hold.getReference().equals(command.transferId().toString())) {
            throw new IllegalArgumentException(
                    "Hold reference " + hold.getReference()
                            + " does not match transferId " + command.transferId());
        }
        if (hold.getAmount().compareTo(command.amount()) != 0) {
            throw new IllegalArgumentException(
                    "Hold amount " + hold.getAmount()
                            + " does not match transfer amount " + command.amount());
        }
        if (!hold.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new CurrencyMismatchException(hold.getCurrency(), command.currency());
        }

        // Deterministic lock ordering to avoid deadlocks: lock the wallet with the
        // smaller UUID first.
        UUID firstId;
        UUID secondId;
        boolean sourceFirst;
        if (command.sourceWalletId().compareTo(command.destWalletId()) < 0) {
            firstId = command.sourceWalletId();
            secondId = command.destWalletId();
            sourceFirst = true;
        } else if (command.sourceWalletId().compareTo(command.destWalletId()) > 0) {
            firstId = command.destWalletId();
            secondId = command.sourceWalletId();
            sourceFirst = false;
        } else {
            throw new IllegalArgumentException("Source and destination wallet must differ");
        }

        Wallet first = walletRepository.findByIdForUpdate(WalletId.of(firstId))
                .orElseThrow(() -> new WalletNotFoundException(firstId));
        Wallet second = walletRepository.findByIdForUpdate(WalletId.of(secondId))
                .orElseThrow(() -> new WalletNotFoundException(secondId));

        Wallet source = sourceFirst ? first : second;
        Wallet dest = sourceFirst ? second : first;

        if (source.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(command.sourceWalletId(), source.getStatus().name());
        }
        if (dest.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(command.destWalletId(), dest.getStatus().name());
        }
        if (!source.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new CurrencyMismatchException(command.currency(), source.getCurrency());
        }
        if (!dest.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new CurrencyMismatchException(command.currency(), dest.getCurrency());
        }

        String transferRef = command.transferId().toString();
        hold.settle();
        source.debitForTransfer(command.amount(), transferRef, "Transfer out");
        dest.creditForTransfer(command.amount(), transferRef, "Transfer in");

        holdRepository.save(hold);
        walletRepository.save(source);
        walletRepository.save(dest);

        return new SettleResult(
                command.transferId(),
                hold.getId(),
                source.getWalletId().value(),
                source.getBalance(),
                dest.getWalletId().value(),
                dest.getBalance(),
                Instant.now()
        );
    }
}
