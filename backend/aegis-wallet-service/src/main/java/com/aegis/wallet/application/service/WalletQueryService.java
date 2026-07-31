package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.port.inbound.GetWalletDetailUseCase;
import com.aegis.wallet.domain.port.inbound.ListWalletsUseCase;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service implementing wallet query use cases.
 */
@Service
@Transactional(readOnly = true)
public class WalletQueryService implements ListWalletsUseCase, GetWalletDetailUseCase {

    private final WalletRepository walletRepository;

    public WalletQueryService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public List<ListWalletsUseCase.Result> listByUser(UUID userId) {
        List<Wallet> wallets = walletRepository.findByUserId(userId);
        return wallets.stream()
                .map(this::toListResult)
                .toList();
    }

    @Override
    public GetWalletDetailUseCase.Result getDetail(UUID walletId, UUID userId) {
        Wallet wallet = walletRepository.findById(WalletId.of(walletId))
                .orElseThrow(() -> new WalletNotFoundException(walletId));

        if (!wallet.getUserId().equals(userId)) {
            throw new WalletNotFoundException(walletId);
        }

        return new GetWalletDetailUseCase.Result(
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

    private ListWalletsUseCase.Result toListResult(Wallet wallet) {
        return new ListWalletsUseCase.Result(
                wallet.getWalletId().value(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.isPremium(),
                wallet.getCreatedAt()
        );
    }
}
