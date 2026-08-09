package com.aegis.wallet.infrastructure.reconciliation;

import com.aegis.wallet.infrastructure.persistence.LedgerEntryJpaEntity;
import com.aegis.wallet.infrastructure.persistence.LedgerEntryJpaRepository;
import com.aegis.wallet.infrastructure.persistence.WalletJpaEntity;
import com.aegis.wallet.infrastructure.persistence.WalletJpaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerReconciliationService")
class LedgerReconciliationServiceTest {

    @Mock
    private WalletJpaRepository walletJpaRepository;

    @Mock
    private LedgerEntryJpaRepository ledgerEntryJpaRepository;

    private SimpleMeterRegistry meterRegistry;
    private LedgerReconciliationService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new LedgerReconciliationService(
                walletJpaRepository, ledgerEntryJpaRepository, meterRegistry, 0);
    }

    @Nested
    @DisplayName("When ledger matches stored balance")
    class WhenLedgerMatches {

        @Test
        @DisplayName("Should report no discrepancies")
        void shouldReportNoDiscrepancies() {
            UUID walletId = UUID.randomUUID();
            when(walletJpaRepository.findAll()).thenReturn(List.of(
                    new WalletJpaEntity(walletId, UUID.randomUUID(), new BigDecimal("150.00"),
                            "EUR", "ACTIVE", Instant.now(), Instant.now(), 0L)));
            when(ledgerEntryJpaRepository.findByWalletIdOrderByCreatedAtAsc(walletId)).thenReturn(List.of(
                    new LedgerEntryJpaEntity(UUID.randomUUID(), walletId, "OPENING",
                            BigDecimal.ZERO, "EUR", "opening", null, Instant.now()),
                    new LedgerEntryJpaEntity(UUID.randomUUID(), walletId, "DEPOSIT",
                            new BigDecimal("100.00"), "EUR", "TXN-1", null, Instant.now()),
                    new LedgerEntryJpaEntity(UUID.randomUUID(), walletId, "DEPOSIT",
                            new BigDecimal("50.00"), "EUR", "TXN-2", null, Instant.now())
            ));
            when(walletJpaRepository.count()).thenReturn(1L);

            List<LedgerReconciliationService.Discrepancy> result = service.reconcileAll();

            assertTrue(result.isEmpty());
            assertEquals(0.0, meterRegistry.get("aegis.wallet.reconciliation_discrepancies").gauge().value());
        }
    }

    @Nested
    @DisplayName("When ledger does not match stored balance")
    class WhenLedgerDoesNotMatch {

        @Test
        @DisplayName("Should report the wallet as a discrepancy")
        void shouldReportDiscrepancy() {
            UUID walletId = UUID.randomUUID();
            when(walletJpaRepository.findAll()).thenReturn(List.of(
                    new WalletJpaEntity(walletId, UUID.randomUUID(), new BigDecimal("90.00"),
                            "EUR", "ACTIVE", Instant.now(), Instant.now(), 0L)));
            when(ledgerEntryJpaRepository.findByWalletIdOrderByCreatedAtAsc(walletId)).thenReturn(List.of(
                    new LedgerEntryJpaEntity(UUID.randomUUID(), walletId, "DEPOSIT",
                            new BigDecimal("100.00"), "EUR", "TXN-1", null, Instant.now())
            ));

            List<LedgerReconciliationService.Discrepancy> result = service.reconcileAll();

            assertEquals(1, result.size());
            assertEquals(walletId, result.get(0).walletId());
            assertEquals(0, new BigDecimal("90.00").compareTo(result.get(0).storedBalance()));
            assertEquals(0, new BigDecimal("100.00").compareTo(result.get(0).ledgerSum()));
            assertEquals(1.0, meterRegistry.get("aegis.wallet.reconciliation_discrepancies").gauge().value());
        }

        @Test
        @DisplayName("Should account for reversal entries as outflows")
        void shouldAccountForReversalsAsOutflows() {
            UUID walletId = UUID.randomUUID();
            UUID depositId = UUID.randomUUID();
            when(walletJpaRepository.findAll()).thenReturn(List.of(
                    new WalletJpaEntity(walletId, UUID.randomUUID(), new BigDecimal("0.00"),
                            "EUR", "ACTIVE", Instant.now(), Instant.now(), 0L)));
            when(ledgerEntryJpaRepository.findByWalletIdOrderByCreatedAtAsc(walletId)).thenReturn(List.of(
                    new LedgerEntryJpaEntity(UUID.randomUUID(), walletId, "DEPOSIT",
                            new BigDecimal("100.00"), "EUR", "TXN-1", null, Instant.now()),
                    new LedgerEntryJpaEntity(UUID.randomUUID(), walletId, "REVERSAL",
                            new BigDecimal("100.00"), "EUR", "REV-1", depositId, Instant.now())
            ));

            List<LedgerReconciliationService.Discrepancy> result = service.reconcileAll();

            assertTrue(result.isEmpty(), "balance 0.00 == 100.00 - 100.00, no discrepancy expected");
        }
    }
}
