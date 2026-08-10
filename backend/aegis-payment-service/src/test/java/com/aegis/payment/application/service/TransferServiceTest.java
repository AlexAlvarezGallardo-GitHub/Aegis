package com.aegis.payment.application.service;

import com.aegis.payment.domain.event.TransferRequested;
import com.aegis.payment.domain.exception.DuplicateTransferException;
import com.aegis.payment.domain.exception.SelfTransferException;
import com.aegis.payment.domain.exception.TransferNotFoundException;
import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.model.TransferStatus;
import com.aegis.payment.domain.port.inbound.TransferFundsUseCase;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.domain.port.outbound.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService - Application Service")
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private EventPublisher eventPublisher;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferService(transferRepository, eventPublisher);
    }

    @Nested
    @DisplayName("When executing transferFunds")
    class WhenExecutingTransferFunds {

        @Test
        @DisplayName("Should persist transfer and publish event")
        void shouldPersistAndPublish() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    source, dest, user, new BigDecimal("50.00"), "EUR", "test", "ref-001");

            when(transferRepository.existsBySourceWalletIdAndReference(source, "ref-001")).thenReturn(false);
            when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

            // The service throws UnsupportedOperationException after saving (scaffold)
            assertThrows(UnsupportedOperationException.class,
                    () -> transferService.execute(command));

            ArgumentCaptor<Transfer> captor = ArgumentCaptor.forClass(Transfer.class);
            verify(transferRepository).save(captor.capture());
            Transfer saved = captor.getValue();
            assertEquals(TransferStatus.PENDING, saved.getStatus());
            assertEquals(source, saved.getSourceWalletId());
            assertEquals(dest, saved.getDestWalletId());

            verify(eventPublisher).publish(any(TransferRequested.class));
        }

        @Test
        @DisplayName("Should throw DuplicateTransferException when reference exists")
        void shouldThrowDuplicate() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    source, dest, UUID.randomUUID(), new BigDecimal("50.00"), "EUR", null, "ref-dup");

            when(transferRepository.existsBySourceWalletIdAndReference(source, "ref-dup")).thenReturn(true);

            assertThrows(DuplicateTransferException.class,
                    () -> transferService.execute(command));
        }

        @Test
        @DisplayName("Should throw SelfTransferException when wallets match")
        void shouldThrowSelfTransfer() {
            UUID wallet = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    wallet, wallet, UUID.randomUUID(), new BigDecimal("50.00"), "EUR", null, "ref-001");

            assertThrows(SelfTransferException.class,
                    () -> transferService.execute(command));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when amount is zero")
        void shouldThrowOnZeroAmount() {
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    BigDecimal.ZERO, "EUR", null, "ref-001");

            assertThrows(IllegalArgumentException.class,
                    () -> transferService.execute(command));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when currency is invalid")
        void shouldThrowOnInvalidCurrency() {
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new BigDecimal("50.00"), "euro", null, "ref-001");

            assertThrows(IllegalArgumentException.class,
                    () -> transferService.execute(command));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when reference is blank")
        void shouldThrowOnBlankReference() {
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    new BigDecimal("50.00"), "EUR", null, "  ");

            assertThrows(IllegalArgumentException.class,
                    () -> transferService.execute(command));
        }
    }

    @Nested
    @DisplayName("When finding a transfer by ID")
    class WhenFindingTransfer {

        @Test
        @DisplayName("Should return transfer when found")
        void shouldReturnTransfer() {
            UUID id = UUID.randomUUID();
            Transfer transfer = Transfer.request(UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), new BigDecimal("50.00"), "EUR", null, "ref-001");
            Transfer rehydrated = Transfer.rehydrate(id, transfer.getSourceWalletId(),
                    transfer.getDestWalletId(), transfer.getUserId(), transfer.getAmount(),
                    transfer.getCurrency(), transfer.getDescription(), transfer.getReference(),
                    transfer.getStatus(), null, null, null,
                    transfer.getCreatedAt(), transfer.getUpdatedAt(), null);

            when(transferRepository.findById(id)).thenReturn(Optional.of(rehydrated));

            Transfer result = transferService.findById(id);
            assertEquals(id, result.getId());
        }

        @Test
        @DisplayName("Should throw TransferNotFoundException when not found")
        void shouldThrowNotFound() {
            UUID id = UUID.randomUUID();
            when(transferRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(TransferNotFoundException.class,
                    () -> transferService.findById(id));
        }
    }
}
