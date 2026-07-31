package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.DuplicateDepositException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.inbound.DepositFundsUseCase;
import com.aegis.wallet.domain.port.outbound.EventPublisher;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DepositFundsService implements DepositFundsUseCase {

    private final WalletRepository walletRepository;
    private final EventPublisher eventPublisher;

    public DepositFundsService(WalletRepository walletRepository,
                                EventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public DepositResult deposit(DepositCommand command) {
        Wallet wallet = walletRepository.findById(WalletId.of(command.walletId()))
                .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

        if (!wallet.getUserId().equals(command.userId())) {
            throw new WalletNotFoundException(command.walletId());
        }

        if (wallet.getLedgerEntries().stream()
                .anyMatch(e -> command.reference().equals(e.reference()))) {
            throw new DuplicateDepositException(command.reference());
        }

        wallet.depositFunds(command.amount(), command.source(), command.reference(), "Deposit");

        walletRepository.save(wallet);

        var event = wallet.toFundsDepositedEvent(
                command.amount(), command.source(), command.reference(), command.correlationId());
        eventPublisher.publish(event);

        UUID depositId = wallet.getLedgerEntries().stream()
                .filter(e -> command.reference().equals(e.reference()))
                .findFirst()
                .map(e -> e.id())
                .orElseThrow(() -> new IllegalStateException("Deposit ledger entry not found"));

        return new DepositResult(
                depositId,
                wallet.getWalletId().value(),
                wallet.getBalance(),
                command.amount(),
                wallet.getCurrency(),
                command.source(),
                command.reference(),
                wallet.getUpdatedAt()
        );
    }
}
