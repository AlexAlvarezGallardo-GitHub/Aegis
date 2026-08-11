package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.InsufficientFundsException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.CreateHoldUseCase;
import com.aegis.wallet.domain.port.outbound.HoldRepository;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CreateHoldService implements CreateHoldUseCase {

    private final WalletRepository walletRepository;
    private final HoldRepository holdRepository;

    public CreateHoldService(WalletRepository walletRepository, HoldRepository holdRepository) {
        this.walletRepository = walletRepository;
        this.holdRepository = holdRepository;
    }

    @Override
    @Transactional
    public HoldResult createHold(CreateHoldCommand command) {
        Wallet wallet = walletRepository.findByIdForUpdate(WalletId.of(command.walletId()))
                .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletNotActiveException(command.walletId(), wallet.getStatus().name());
        }

        if (!wallet.getCurrency().equalsIgnoreCase(command.currency())) {
            throw new CurrencyMismatchException(wallet.getCurrency(), command.currency());
        }

        // Idempotency: if an ACTIVE hold with the same reference already exists, return it.
        Hold existing = holdRepository.findActiveByReference(command.reference()).orElse(null);
        if (existing != null) {
            BigDecimal available = computeAvailable(wallet);
            return toResult(existing, available);
        }

        BigDecimal reservedTotal = holdRepository.sumActiveAmountByWalletId(command.walletId());
        BigDecimal available = wallet.getBalance().subtract(reservedTotal);

        if (command.amount().compareTo(available) > 0) {
            throw new InsufficientFundsException(
                    "Insufficient available balance. Available: " + available
                            + ", requested: " + command.amount());
        }

        Hold hold = Hold.reserve(
                command.walletId(),
                command.amount(),
                command.currency(),
                command.reference()
        );
        Hold saved = holdRepository.save(hold);

        BigDecimal newAvailable = available.subtract(saved.getAmount());
        return toResult(saved, newAvailable);
    }

    private BigDecimal computeAvailable(Wallet wallet) {
        BigDecimal reserved = holdRepository.sumActiveAmountByWalletId(wallet.getWalletId().value());
        return wallet.getBalance().subtract(reserved);
    }

    private HoldResult toResult(Hold hold, BigDecimal availableBalance) {
        return new HoldResult(
                hold.getId(),
                hold.getWalletId(),
                hold.getAmount(),
                hold.getCurrency(),
                hold.getReference(),
                hold.getStatus().name(),
                availableBalance,
                hold.getCreatedAt(),
                hold.getExpiresAt()
        );
    }
}
