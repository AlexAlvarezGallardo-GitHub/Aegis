package com.aegis.wallet.domain.model;

import com.aegis.common.util.UuidV7Generator;

import java.util.Objects;
import java.util.UUID;

public record WalletId(UUID value) {

    public WalletId {
        Objects.requireNonNull(value, "WalletId value must not be null");
    }

    public static WalletId generate() {
        return new WalletId(UuidV7Generator.generate());
    }

    public static WalletId of(UUID value) {
        return new WalletId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
