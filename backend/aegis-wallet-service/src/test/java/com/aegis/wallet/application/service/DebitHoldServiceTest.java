package com.aegis.wallet.application.service;

import com.aegis.wallet.domain.exception.CurrencyMismatchException;
import com.aegis.wallet.domain.exception.HoldNotActiveException;
import com.aegis.wallet.domain.exception.HoldNotFoundException;
import com.aegis.wallet.domain.exception.WalletNotActiveException;
import com.aegis.wallet.domain.exception.WalletNotFoundException;
import com.aegis.wallet.domain.model.Hold;
import com.aegis.wallet.domain.model.HoldStatus;
import com.aegis.wallet.domain.model.Wallet;
import com.aegis.wallet.domain.model.WalletId;
import com.aegis.wallet.domain.model.WalletStatus;
import com.aegis.wallet.domain.port.inbound.DebitHoldUseCase;
import com.aegis.wallet.domain.port.outbound.HoldRepository;
import com.aegis.wallet.domain.port.outbound.WalletRepository;
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
@DisplayName("DebitHoldService - Payment Debit Service")
class DebitHoldServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private HoldRepository holdRepository;

    private DebitHoldService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new DebitHoldService(walletRepository, holdRepository);
    }

    @Nested
    @DisplayName("When debiting a hold for a payment")
    class WhenDebitingHold {

        @Test
        @DisplayName("Should debit wallet and settle hold on success")
        void shouldDebitOnSuccess() {
            UUID paymentId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();

            Hold hold = Hold.rehydrate(holdId, walletId, new BigDecimal("25.00"), "EUR",
                    paymentId.toString(), HoldStatus.ACTIVE, Instant.now(), Instant.now().plusSeconds(300));
            Wallet wallet = Wallet.rehydrate(WalletId.of(walletId), UUID.randomUUID(),
                    new BigDecimal("100.00"), "EUR", WalletStatus.ACTIVE,
                    Instant.now(), Instant.now(), 0, java.util.List.of());

            when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
            when(walletRepository.findByIdForUpdate(WalletId.of(walletId))).thenReturn(Optional.of(wallet));
            when(holdRepository.save(any(Hold.class))).thenAnswer(inv -> inv.getArgument(0));
            when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

            DebitHoldUseCase.DebitResult result = service.debit(
                    new DebitHoldUseCase.DebitCommand(paymentId, holdId, walletId,
                            new BigDecimal("25.00"), "EUR"));

            assertEquals(paymentId, result.paymentId());
            assertEquals(holdId, result.holdId());
            assertEquals(walletId, result.walletId());
            assertEquals(0, new BigDecimal("75.00").compareTo(result.newBalance()));
            verify(holdRepository).save(hold);
            verify(walletRepository).save(wallet);
        }

        @Test
        @DisplayName("Should throw HoldNotFoundException when hold does not exist")
        void shouldThrowWhenHoldNotFound() {
            UUID holdId = UUID.randomUUID();
            when(holdRepository.findById(holdId)).thenReturn(Optional.empty());

            assertThrows(HoldNotFoundException.class,
                    () -> service.debit(new DebitHoldUseCase.DebitCommand(
                            UUID.randomUUID(), holdId, UUID.randomUUID(),
                            new BigDecimal("25.00"), "EUR")));
        }

        @Test
        @DisplayName("Should throw HoldNotActiveException when hold is already settled")
        void shouldThrowWhenHoldNotActive() {
            UUID holdId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();
            Hold hold = Hold.rehydrate(holdId, walletId, new BigDecimal("25.00"), "EUR",
                    UUID.randomUUID().toString(), HoldStatus.SETTLED,
                    Instant.now(), Instant.now().plusSeconds(300));

            when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));

            assertThrows(HoldNotActiveException.class,
                    () -> service.debit(new DebitHoldUseCase.DebitCommand(
                            UUID.randomUUID(), holdId, walletId,
                            new BigDecimal("25.00"), "EUR")));
        }

        @Test
        @DisplayName("Should throw WalletNotFoundException when wallet does not exist")
        void shouldThrowWhenWalletNotFound() {
            UUID paymentId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();

            Hold hold = Hold.rehydrate(holdId, walletId, new BigDecimal("25.00"), "EUR",
                    paymentId.toString(), HoldStatus.ACTIVE,
                    Instant.now(), Instant.now().plusSeconds(300));

            when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
            when(walletRepository.findByIdForUpdate(WalletId.of(walletId))).thenReturn(Optional.empty());

            assertThrows(WalletNotFoundException.class,
                    () -> service.debit(new DebitHoldUseCase.DebitCommand(
                            paymentId, holdId, walletId, new BigDecimal("25.00"), "EUR")));
        }

        @Test
        @DisplayName("Should throw CurrencyMismatchException when currencies differ")
        void shouldThrowOnCurrencyMismatch() {
            UUID paymentId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();

            Hold hold = Hold.rehydrate(holdId, walletId, new BigDecimal("25.00"), "EUR",
                    paymentId.toString(), HoldStatus.ACTIVE,
                    Instant.now(), Instant.now().plusSeconds(300));
            Wallet wallet = Wallet.rehydrate(WalletId.of(walletId), UUID.randomUUID(),
                    new BigDecimal("100.00"), "USD", WalletStatus.ACTIVE,
                    Instant.now(), Instant.now(), 0, java.util.List.of());

            when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
            when(walletRepository.findByIdForUpdate(WalletId.of(walletId))).thenReturn(Optional.of(wallet));

            assertThrows(CurrencyMismatchException.class,
                    () -> service.debit(new DebitHoldUseCase.DebitCommand(
                            paymentId, holdId, walletId, new BigDecimal("25.00"), "EUR")));
        }

        @Test
        @DisplayName("Should return existing result when hold already settled for same payment (idempotent)")
        void shouldBeIdempotent() {
            UUID paymentId = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            UUID walletId = UUID.randomUUID();

            Hold hold = Hold.rehydrate(holdId, walletId, new BigDecimal("25.00"), "EUR",
                    paymentId.toString(), HoldStatus.SETTLED,
                    Instant.now(), Instant.now().plusSeconds(300));
            Wallet wallet = Wallet.rehydrate(WalletId.of(walletId), UUID.randomUUID(),
                    new BigDecimal("75.00"), "EUR", WalletStatus.ACTIVE,
                    Instant.now(), Instant.now(), 0, java.util.List.of());

            when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
            when(walletRepository.findById(WalletId.of(walletId))).thenReturn(Optional.of(wallet));

            DebitHoldUseCase.DebitResult result = service.debit(
                    new DebitHoldUseCase.DebitCommand(paymentId, holdId, walletId,
                            new BigDecimal("25.00"), "EUR"));

            assertEquals(0, new BigDecimal("75.00").compareTo(result.newBalance()));
        }
    }
}
