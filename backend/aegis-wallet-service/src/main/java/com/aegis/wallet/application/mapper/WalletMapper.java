package com.aegis.wallet.application.mapper;

import com.aegis.wallet.application.dto.WalletResponse;
import com.aegis.wallet.domain.model.Wallet;

import java.util.List;

public final class WalletMapper {

    private WalletMapper() {}

    public static WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getWalletId().value(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus().name(),
                wallet.getCreatedAt()
        );
    }

    public static List<WalletResponse> toResponseList(List<Wallet> wallets) {
        return wallets.stream()
                .map(WalletMapper::toResponse)
                .toList();
    }
}
