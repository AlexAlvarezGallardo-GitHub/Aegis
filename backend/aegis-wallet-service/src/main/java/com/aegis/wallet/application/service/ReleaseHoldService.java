package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.HoldNotFoundException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.HoldStatus;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.inbound.ReleaseHoldUseCase;
import com.aegis.wallet.domain.port.outbound.HoldRepository;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ReleaseHoldService implements ReleaseHoldUseCase {

    private final WalletRepository walletRepository;
    private final HoldRepository holdRepository;

    public ReleaseHoldService(WalletRepository walletRepository, HoldRepository holdRepository) {
        this.walletRepository = walletRepository;
        this.holdRepository = holdRepository;
    }

    @Override
    @Transactional
    public HoldResult release(ReleaseCommand command) {
        Hold hold = holdRepository.findById(command.holdId())
                .orElseThrow(() -> new HoldNotFoundException(command.holdId()));

        if (!hold.getWalletId().equals(command.walletId())) {
            throw new HoldNotFoundException(command.holdId());
        }

        Wallet wallet = walletRepository.findById(WalletId.of(command.walletId()))
                .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

        // If already released, return current state (idempotent).
        if (hold.getStatus() == HoldStatus.RELEASED) {
            BigDecimal available = computeAvailable(wallet);
            return toResult(hold, available);
        }

        // ACTIVE → RELEASED; SETTLED/EXPIRED → HoldNotActiveException
        hold.release();
        holdRepository.save(hold);

        BigDecimal available = computeAvailable(wallet);
        return toResult(hold, available);
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
