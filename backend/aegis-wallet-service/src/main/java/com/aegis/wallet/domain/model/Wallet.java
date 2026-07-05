package com.aegis.wallet.domain.model;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.wallet.domain.event.WalletCreated;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Wallet {

    private final WalletId walletId;
    private final UUID userId;
    private BigDecimal balance;
    private final String currency;
    private WalletStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;
    private final List<LedgerEntry> ledgerEntries;

    private Wallet(WalletId walletId, UUID userId, String currency) {
        this.walletId = Objects.requireNonNull(walletId, "WalletId must not be null");
        this.userId = Objects.requireNonNull(userId, "UserId must not be null");
        this.balance = BigDecimal.ZERO;
        this.currency = normalizeCurrency(currency);
        this.status = WalletStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.version = 0;
        this.ledgerEntries = new ArrayList<>();
    }

    private Wallet(WalletId walletId, UUID userId, BigDecimal balance, String currency,
                   WalletStatus status, Instant createdAt, Instant updatedAt, long version,
                   List<LedgerEntry> ledgerEntries) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.ledgerEntries = new ArrayList<>(ledgerEntries);
    }

    public static Wallet create(UUID userId, String currency) {
        WalletId walletId = WalletId.generate();
        Wallet wallet = new Wallet(walletId, userId, currency);
        wallet.addOpeningEntry();
        return wallet;
    }

    public static Wallet rehydrate(WalletId walletId, UUID userId, BigDecimal balance, String currency,
                                    WalletStatus status, Instant createdAt, Instant updatedAt, long version,
                                    List<LedgerEntry> ledgerEntries) {
        return new Wallet(walletId, userId, balance, currency, status, createdAt, updatedAt, version, ledgerEntries);
    }

    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }
        String normalized = currency.trim().toUpperCase();
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency: " + normalized);
        }
        return normalized;
    }

    private void addOpeningEntry() {
        LedgerEntry entry = new LedgerEntry(
                UuidV7Generator.generate(),
                walletId.value(),
                LedgerEntryType.OPENING,
                BigDecimal.ZERO,
                currency,
                "Wallet created",
                createdAt
        );
        ledgerEntries.add(entry);
    }

    public WalletCreated toCreatedEvent(String correlationId) {
        return new WalletCreated(
                walletId.value(),
                userId,
                currency,
                createdAt,
                correlationId
        );
    }

    public WalletId getWalletId() { return walletId; }
    public UUID getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public WalletStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public List<LedgerEntry> getLedgerEntries() { return List.copyOf(ledgerEntries); }
}
