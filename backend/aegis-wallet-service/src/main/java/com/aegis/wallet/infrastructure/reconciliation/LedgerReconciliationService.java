package com.aegis.wallet.infrastructure.reconciliation;

import com.aegis.wallet.domain.model.LedgerEntryType;
import com.aegis.wallet.infrastructure.persistence.LedgerEntryJpaEntity;
import com.aegis.wallet.infrastructure.persistence.LedgerEntryJpaRepository;
import com.aegis.wallet.infrastructure.persistence.WalletJpaEntity;
import com.aegis.wallet.infrastructure.persistence.WalletJpaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reconciles each wallet's stored balance against the sum of its immutable
 * ledger entries (ADR-004 invariant: balance == inflows - outflows).
 *
 * <p>Discrepancies are logged and exposed via the
 * {@code aegis.wallet.reconciliation_discrepancies} gauge so operators can alert
 * on drift between the derived projection and the ledger source of truth.</p>
 */
@Component
public class LedgerReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(LedgerReconciliationService.class);

    private static final Set<LedgerEntryType> INFLOW_TYPES = EnumSet.of(
            LedgerEntryType.OPENING, LedgerEntryType.DEPOSIT,
            LedgerEntryType.TRANSFER_IN, LedgerEntryType.REFUND);

    private static final Set<LedgerEntryType> OUTFLOW_TYPES = EnumSet.of(
            LedgerEntryType.WITHDRAWAL, LedgerEntryType.TRANSFER_OUT,
            LedgerEntryType.PAYMENT, LedgerEntryType.REVERSAL);

    private final WalletJpaRepository walletJpaRepository;
    private final LedgerEntryJpaRepository ledgerEntryJpaRepository;
    private final long discrepancyLimit;
    private final AtomicLong discrepancyCount = new AtomicLong();

    public LedgerReconciliationService(WalletJpaRepository walletJpaRepository,
                                       LedgerEntryJpaRepository ledgerEntryJpaRepository,
                                       MeterRegistry meterRegistry,
                                       @Value("${aegis.wallet.reconciliation.discrepancy-limit:0}") long discrepancyLimit) {
        this.walletJpaRepository = walletJpaRepository;
        this.ledgerEntryJpaRepository = ledgerEntryJpaRepository;
        this.discrepancyLimit = discrepancyLimit;
        meterRegistry.gauge("aegis.wallet.reconciliation_discrepancies", discrepancyCount,
                AtomicLong::get);
    }

    /**
     * Runs the reconciliation over all wallets and returns the discrepancies found.
     *
     * @return list of wallets whose stored balance differs from the ledger sum
     */
    @Scheduled(fixedDelayString = "${aegis.wallet.reconciliation.interval-ms:60000}")
    @Transactional(readOnly = true)
    public List<Discrepancy> reconcileAll() {
        List<Discrepancy> discrepancies = new ArrayList<>();

        for (WalletJpaEntity wallet : walletJpaRepository.findAll()) {
            BigDecimal ledgerSum = ledgerEntryJpaRepository
                    .findByWalletIdOrderByCreatedAtAsc(wallet.getId()).stream()
                    .map(this::signedAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (wallet.getBalance().compareTo(ledgerSum) != 0) {
                Discrepancy d = new Discrepancy(wallet.getId(), wallet.getBalance(), ledgerSum);
                discrepancies.add(d);
                log.warn("Ledger reconciliation discrepancy: wallet={}, storedBalance={}, ledgerSum={}",
                        wallet.getId(), wallet.getBalance(), ledgerSum);
            }
        }

        if (discrepancies.isEmpty()) {
            log.info("Ledger reconciliation OK for {} wallets", walletJpaRepository.count());
        }

        discrepancyCount.set(discrepancies.size());
        if (discrepancies.size() > discrepancyLimit) {
            log.error("Ledger reconciliation exceeded discrepancy limit: {} (limit {})",
                    discrepancies.size(), discrepancyLimit);
        }
        return discrepancies;
    }

    private BigDecimal signedAmount(LedgerEntryJpaEntity entry) {
        LedgerEntryType type = LedgerEntryType.valueOf(entry.getType());
        return INFLOW_TYPES.contains(type) ? entry.getAmount() : entry.getAmount().negate();
    }

    /**
     * A discrepancy between a wallet's stored balance and its ledger sum.
     */
    public record Discrepancy(UUID walletId, BigDecimal storedBalance, BigDecimal ledgerSum) {
    }
}
