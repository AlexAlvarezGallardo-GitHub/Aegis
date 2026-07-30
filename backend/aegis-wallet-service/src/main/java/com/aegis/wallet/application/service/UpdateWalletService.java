package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.event.WalletBalanceAdjusted;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.inbound.UpdateWalletUseCase;
import com.aegis.wallet.domain.port.outbound.EventPublisher;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class UpdateWalletService implements UpdateWalletUseCase {

    private final WalletRepository walletRepository;
    private final EventPublisher eventPublisher;

    public UpdateWalletService(WalletRepository walletRepository,
                                EventPublisher eventPublisher) {
        this.walletRepository = walletRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public WalletDetailResult adjustBalance(AdjustBalanceCommand command) {
        Wallet wallet = findAndValidateOwnership(command.walletId(), command.userId());

        BigDecimal previousBalance = wallet.getBalance();
        wallet.adjustBalance(command.amount(), command.description());

        Wallet saved = walletRepository.save(wallet);

        WalletBalanceAdjusted event = saved.toBalanceAdjustedEvent(
                previousBalance, command.amount(), command.description(), command.correlationId());
        eventPublisher.publish(event);

        return toResult(saved);
    }

    @Override
    @Transactional
    public WalletDetailResult changeStatus(StatusChangeCommand command) {
        Wallet wallet = findAndValidateOwnership(command.walletId(), command.userId());

        wallet.deactivate(command.newStatus());

        Wallet saved = walletRepository.save(wallet);

        return toResult(saved);
    }

    public WalletDetailResult getDetail(UUID walletId, UUID userId) {
        Wallet wallet = findAndValidateOwnership(walletId, userId);
        return toResult(wallet);
    }

    private Wallet findAndValidateOwnership(UUID walletId, UUID userId) {
        Wallet wallet = walletRepository.findById(WalletId.of(walletId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        if (!wallet.getUserId().equals(userId)) {
            throw new WalletNotFoundException(walletId);
        }

        return wallet;
    }

    private WalletDetailResult toResult(Wallet wallet) {
        return new WalletDetailResult(
                wallet.getWalletId().value(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.isPremium(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
