package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.LedgerEntryType;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.CreditRefundUseCase;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditRefundService - Refund Credit Service")
class CreditRefundServiceTest {

    @Mock private WalletRepository walletRepository;

    private CreditRefundService service;

    @BeforeEach
    void setUp() {
        service = new CreditRefundService(walletRepository);
    }

    @Nested
    @DisplayName("When crediting a wallet for a refund")
    class WhenCreditingRefund {

        @Test
        @DisplayName("Should credit wallet and create REFUND ledger entry")
        void shouldCreditWallet() {
            UUID refundId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();

            Wallet wallet = Wallet.rehydrate(WalletId.of(walletId), UUID.randomUUID(),
                    new BigDecimal("75.00"), "EUR", WalletStatus.ACTIVE,
                    Instant.now(), Instant.now(), 0, java.util.List.of());

            when(walletRepository.findByIdForUpdate(WalletId.of(walletId))).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            CreditRefundUseCase.CreditResult result = service.credit(
                    new CreditRefundUseCase.CreditCommand(refundId, walletId,
                            new BigDecimal("25.00"), "EUR"));

            assertEquals(refundId, result.refundId());
            assertEquals(walletId, result.walletId());
            assertEquals(0, new BigDecimal("100.00").compareTo(result.newBalance()));
            verify(walletRepository).save(wallet);
        }

        @Test
        @DisplayName("Should throw WalletNotFoundException when wallet does not exist")
        void shouldThrowWhenWalletNotFound() {
            UUID walletId = UUID.randomUUID();
            when(walletRepository.findByIdForUpdate(WalletId.of(walletId))).thenReturn(Optional.empty());

            assertThrows(WalletNotFoundException.class,
                    () -> service.credit(new CreditRefundUseCase.CreditCommand(
                            UUID.randomUUID(), walletId, new BigDecimal("25.00"), "EUR")));
        }

        @Test
        @DisplayName("Should throw WalletNotActiveException when wallet is not ACTIVE")
        void shouldThrowWhenWalletNotActive() {
            UUID walletId = UUID.randomUUID();
            Wallet wallet = Wallet.rehydrate(WalletId.of(walletId), UUID.randomUUID(),
                    new BigDecimal("100.00"), "EUR", WalletStatus.FROZEN,
                    Instant.now(), Instant.now(), 0, java.util.List.of());

            when(walletRepository.findByIdForUpdate(WalletId.of(walletId))).thenReturn(Optional.of(wallet));

            assertThrows(WalletNotActiveException.class,
                    () -> service.credit(new CreditRefundUseCase.CreditCommand(
                            UUID.randomUUID(), walletId, new BigDecimal("25.00"), "EUR")));
        }

        @Test
        @DisplayName("Should throw CurrencyMismatchException when currency does not match")
        void shouldThrowWhenCurrencyMismatch() {
            UUID walletId = UUID.randomUUID();
            Wallet wallet = Wallet.rehydrate(WalletId.of(walletId), UUID.randomUUID(),
                    new BigDecimal("100.00"), "EUR", WalletStatus.ACTIVE,
                    Instant.now(), Instant.now(), 0, java.util.List.of());

            when(walletRepository.findByIdForUpdate(WalletId.of(walletId))).thenReturn(Optional.of(wallet));

            assertThrows(CurrencyMismatchException.class,
                    () -> service.credit(new CreditRefundUseCase.CreditCommand(
                            UUID.randomUUID(), walletId, new BigDecimal("25.00"), "USD")));
        }

        @Test
        @DisplayName("Should be idempotent for duplicate refundId")
        void shouldBeIdempotent() {
            UUID refundId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();

            Wallet wallet = Wallet.rehydrate(WalletId.of(walletId), UUID.randomUUID(),
                    new BigDecimal("75.00"), "EUR", WalletStatus.ACTIVE,
                    Instant.now(), Instant.now(), 0, java.util.List.of());

            when(walletRepository.findByIdForUpdate(WalletId.of(walletId))).thenReturn(Optional.of(wallet));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            // First credit
            service.credit(new CreditRefundUseCase.CreditCommand(refundId, walletId,
                    new BigDecimal("25.00"), "EUR"));
            BigDecimal balanceAfterFirst = wallet.getBalance();

            // Second credit with same refundId (idempotent)
            CreditRefundUseCase.CreditResult result = service.credit(
                    new CreditRefundUseCase.CreditCommand(refundId, walletId,
                            new BigDecimal("25.00"), "EUR"));

            assertEquals(0, balanceAfterFirst.compareTo(result.newBalance()));
        }
    }
}
