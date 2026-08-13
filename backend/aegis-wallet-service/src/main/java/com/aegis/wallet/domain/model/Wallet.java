package com.aegis.wallet.domain.model;

import com.aegis.common.util.UuidV7Generator;
import com.aegis.wallet.domain.event.FundsDeposited;
import com.aegis.wallet.domain.event.WalletBalanceAdjusted;
import com.aegis.wallet.domain.event.WalletCreated;
import com.aegis.wallet.domain.exception.InsufficientFundsException;
import com.aegis.wallet.domain.exception.WalletOperationNotAllowedException;

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

    /**
     * Adjusts the wallet balance by the given amount.
     *
     * <p>If the amount is negative (withdrawal) and the resulting balance would be negative,
     * an {@link InsufficientFundsException} is thrown.</p>
     *
     * @param amount      the adjustment amount (positive for deposit, negative for withdrawal)
     * @param description an optional description for the ledger entry
     * @throws InsufficientFundsException if the resulting balance would be negative
     */
    public void adjustBalance(BigDecimal amount, String description) {
        if (status != WalletStatus.ACTIVE) {
            throw new WalletOperationNotAllowedException(
                    "Cannot modify balance. Wallet is " + status.name().toLowerCase());
        }
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Amount must not be zero");
        }

        BigDecimal newBalance = this.balance.add(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Current balance: " + this.balance + ", requested: " + amount);
        }

        this.balance = newBalance;

        LedgerEntryType type = amount.compareTo(BigDecimal.ZERO) > 0
                ? LedgerEntryType.DEPOSIT
                : LedgerEntryType.WITHDRAWAL;

        LedgerEntry entry = new LedgerEntry(
                UuidV7Generator.generate(),
                walletId.value(),
                type,
                amount.abs(),
                currency,
                description != null ? description : type == LedgerEntryType.DEPOSIT ? "Deposit" : "Withdrawal",
                Instant.now()
        );
        ledgerEntries.add(entry);
        this.updatedAt = Instant.now();
    }

    public void depositFunds(BigDecimal amount, String source, String reference, String description) {
        if (status != WalletStatus.ACTIVE) {
            throw new WalletOperationNotAllowedException(
                    "Cannot deposit. Wallet is " + status.name().toLowerCase());
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        this.balance = this.balance.add(amount);

        LedgerEntry entry = new LedgerEntry(
                UuidV7Generator.generate(),
                walletId.value(),
                LedgerEntryType.DEPOSIT,
                amount,
                currency,
                reference != null ? reference : description != null ? description : "Deposit",
                Instant.now()
        );
        ledgerEntries.add(entry);
        this.updatedAt = Instant.now();
    }

    public void deactivate(WalletStatus targetStatus) {
        if (targetStatus == WalletStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot deactivate to ACTIVE status");
        }
        if (this.balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new WalletOperationNotAllowedException(
                    "Cannot deactivate wallet with non-zero balance: " + this.balance);
        }
        this.status = targetStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Reverses a previously applied deposit by appending an immutable REVERSAL
     * entry that references the original entry. The original entry is never
     * modified (ADR-004).
     *
     * @param depositEntryId the id of the original DEPOSIT entry
     * @param reference      idempotency reference for the reversal
     * @param description    optional description for the reversal entry
     * @return the newly appended REVERSAL entry
     * @throws com.aegis.wallet.domain.exception.DepositReversalException if the
     *         deposit is not found, already reversed, or the wallet is inactive
     */
    public LedgerEntry reverseDeposit(UUID depositEntryId, String reference, String description) {
        if (status != WalletStatus.ACTIVE) {
            throw new com.aegis.wallet.domain.exception.DepositReversalException(
                    "Cannot reverse deposit. Wallet is " + status.name().toLowerCase());
        }

        LedgerEntry original = ledgerEntries.stream()
                .filter(e -> e.id().equals(depositEntryId))
                .findFirst()
                .orElseThrow(() -> new com.aegis.wallet.domain.exception.DepositReversalException(
                        "Deposit entry not found: " + depositEntryId));

        if (original.type() != LedgerEntryType.DEPOSIT) {
            throw new com.aegis.wallet.domain.exception.DepositReversalException(
                    "Entry " + depositEntryId + " is not a DEPOSIT");
        }

        boolean alreadyReversed = ledgerEntries.stream()
                .anyMatch(e -> e.type() == LedgerEntryType.REVERSAL
                        && depositEntryId.equals(e.reversalOf()));
        if (alreadyReversed) {
            throw new com.aegis.wallet.domain.exception.DepositReversalException(
                    "Deposit entry already reversed: " + depositEntryId);
        }

        this.balance = this.balance.subtract(original.amount());

        LedgerEntry reversal = new LedgerEntry(
                UuidV7Generator.generate(),
                walletId.value(),
                LedgerEntryType.REVERSAL,
                original.amount(),
                original.currency(),
                reference != null ? reference : description != null ? description : "Reversal of " + depositEntryId,
                Instant.now(),
                depositEntryId
        );
        ledgerEntries.add(reversal);
        this.updatedAt = Instant.now();
        return reversal;
    }

    /**
     * Debits the wallet for an outgoing transfer, appending a TRANSFER_OUT ledger entry.
     *
     * @param amount      the transfer amount (strictly positive; stored as absolute value)
     * @param reference   the transfer id
     * @param description optional description for the ledger entry
     * @throws WalletOperationNotAllowedException if the wallet is not ACTIVE
     * @throws InsufficientFundsException         if the debit would make the balance negative
     * @throws IllegalArgumentException           if the amount is not positive
     */
    public void debitForTransfer(BigDecimal amount, String reference, String description) {
        if (status != WalletStatus.ACTIVE) {
            throw new WalletOperationNotAllowedException(
                    "Cannot debit for transfer. Wallet is " + status.name().toLowerCase());
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        BigDecimal newBalance = this.balance.subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds for transfer. Current balance: " + this.balance
                            + ", requested: " + amount);
        }

        this.balance = newBalance;

        LedgerEntry entry = new LedgerEntry(
                UuidV7Generator.generate(),
                walletId.value(),
                LedgerEntryType.TRANSFER_OUT,
                amount,
                currency,
                reference != null ? reference : description != null ? description : "Transfer out",
                Instant.now()
        );
        ledgerEntries.add(entry);
        this.updatedAt = Instant.now();
    }

    /**
     * Credits the wallet for an incoming transfer, appending a TRANSFER_IN ledger entry.
     *
     * @param amount      the transfer amount (strictly positive)
     * @param reference   the transfer id
     * @param description optional description for the ledger entry
     * @throws WalletOperationNotAllowedException if the wallet is not ACTIVE
     * @throws IllegalArgumentException           if the amount is not positive
     */
    public void creditForTransfer(BigDecimal amount, String reference, String description) {
        if (status != WalletStatus.ACTIVE) {
            throw new WalletOperationNotAllowedException(
                    "Cannot credit for transfer. Wallet is " + status.name().toLowerCase());
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        this.balance = this.balance.add(amount);

        LedgerEntry entry = new LedgerEntry(
                UuidV7Generator.generate(),
                walletId.value(),
                LedgerEntryType.TRANSFER_IN,
                amount,
                currency,
                reference != null ? reference : description != null ? description : "Transfer in",
                Instant.now()
        );
        ledgerEntries.add(entry);
        this.updatedAt = Instant.now();
    }

    /**
     * Debits the wallet for a payment, appending a PAYMENT ledger entry.
     *
     * @param amount      the payment amount (strictly positive; stored as absolute value)
     * @param reference   the payment id
     * @param description optional description for the ledger entry
     * @throws WalletOperationNotAllowedException if the wallet is not ACTIVE
     * @throws InsufficientFundsException         if the debit would make the balance negative
     * @throws IllegalArgumentException           if the amount is not positive
     */
    public void debitForPayment(BigDecimal amount, String reference, String description) {
        if (status != WalletStatus.ACTIVE) {
            throw new WalletOperationNotAllowedException(
                    "Cannot debit for payment. Wallet is " + status.name().toLowerCase());
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        BigDecimal newBalance = this.balance.subtract(amount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds for payment. Current balance: " + this.balance
                            + ", requested: " + amount);
        }

        this.balance = newBalance;

        LedgerEntry entry = new LedgerEntry(
                UuidV7Generator.generate(),
                walletId.value(),
                LedgerEntryType.PAYMENT,
                amount,
                currency,
                reference != null ? reference : description != null ? description : "Payment",
                Instant.now()
        );
        ledgerEntries.add(entry);
        this.updatedAt = Instant.now();
    }

    /**
     * Credits the wallet for a refund, appending a REFUND ledger entry.
     * Idempotent by reference: if a REFUND entry with the same reference already exists,
     * returns without creating a duplicate entry (ADR-004).
     *
     * @param amount      the refund amount (strictly positive)
     * @param reference   the refund id (idempotency key)
     * @param description optional description for the ledger entry
     * @throws WalletOperationNotAllowedException if the wallet is not ACTIVE
     * @throws IllegalArgumentException           if the amount is not positive
     */
    public void creditForRefund(BigDecimal amount, String reference, String description) {
        if (status != WalletStatus.ACTIVE) {
            throw new WalletOperationNotAllowedException(
                    "Cannot credit for refund. Wallet is " + status.name().toLowerCase());
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        // Idempotency check: if a REFUND entry with the same reference exists, do nothing
        boolean alreadyCredited = ledgerEntries.stream()
                .anyMatch(e -> e.type() == LedgerEntryType.REFUND
                        && reference != null && reference.equals(e.reference()));
        if (alreadyCredited) {
            return;
        }

        this.balance = this.balance.add(amount);

        LedgerEntry entry = new LedgerEntry(
                UuidV7Generator.generate(),
                walletId.value(),
                LedgerEntryType.REFUND,
                amount,
                currency,
                reference != null ? reference : description != null ? description : "Refund",
                Instant.now()
        );
        ledgerEntries.add(entry);
        this.updatedAt = Instant.now();
    }

    public boolean isPremium() {
        return "EUR".equals(this.currency) && this.balance.compareTo(new BigDecimal("1000")) > 0;
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

    public FundsDeposited toFundsDepositedEvent(BigDecimal amount, String source, String reference,
                                                  String correlationId) {
        return new FundsDeposited(
                walletId.value(),
                userId,
                amount,
                currency,
                source,
                reference,
                balance,
                correlationId
        );
    }

    public WalletBalanceAdjusted toBalanceAdjustedEvent(BigDecimal previousBalance, BigDecimal amount,
                                                        String description, String correlationId) {
        return new WalletBalanceAdjusted(
                walletId.value(),
                userId,
                previousBalance,
                balance,
                amount,
                currency,
                description,
                correlationId
        );
    }

    /**
     * Returns an unmodifiable copy of the ledger entries.
     *
     * <p><strong>Limitation:</strong> All ledger entries are loaded into memory. For wallets with
     * a very large number of transactions, consider paginating ledger entries at the persistence
     * layer (e.g., via a dedicated {@code LedgerEntryRepository.findAllByWalletId(walletId, Pageable)}).
     * This is not yet implemented and should be addressed in a future iteration.</p>
     *
     * @return an unmodifiable list of ledger entries
     */
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
