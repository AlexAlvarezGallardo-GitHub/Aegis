package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.DepositReversalException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.LedgerEntry;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.inbound.ReverseDepositUseCase;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReverseDepositService implements ReverseDepositUseCase {

    private final WalletRepository walletRepository;

    public ReverseDepositService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public ReverseResult reverse(ReverseCommand command) {
        Wallet wallet = walletRepository.findById(WalletId.of(command.walletId()))
                .orElseThrow(() -> new WalletNotFoundException(command.walletId()));

        if (!wallet.getUserId().equals(command.userId())) {
            throw new WalletNotFoundException(command.walletId());
        }

        LedgerEntry reversal;
        try {
            reversal = wallet.reverseDeposit(
                    command.depositEntryId(), command.reference(), "Reversal of deposit");
        } catch (DepositReversalException e) {
            throw e;
        }

        walletRepository.save(wallet);

        return new ReverseResult(
                reversal.id(),
                wallet.getWalletId().value(),
                wallet.getBalance(),
                reversal.amount(),
                wallet.getCurrency(),
                reversal.timestamp()
        );
    }
}
