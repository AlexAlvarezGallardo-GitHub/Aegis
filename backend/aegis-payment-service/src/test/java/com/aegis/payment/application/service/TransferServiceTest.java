package com.aegis.payment.application.service;

import com.aegis.payment.domain.event.TransferCompleted;
import com.aegis.payment.domain.event.TransferFailed;
import com.aegis.payment.domain.event.TransferRequested;
import com.aegis.payment.domain.exception.DuplicateTransferException;
import com.aegis.payment.domain.exception.FraudAssessmentUnavailableException;
import com.aegis.payment.domain.exception.FraudRejectedException;
import com.aegis.payment.domain.exception.SelfTransferException;
import com.aegis.payment.domain.exception.SettlementFailedException;
import com.aegis.payment.domain.exception.TransferNotFoundException;
import com.aegis.payment.domain.model.Transfer;
import com.aegis.payment.domain.model.TransferStatus;
import com.aegis.payment.domain.port.inbound.TransferFundsUseCase;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway.FraudDecision;
import com.aegis.payment.domain.port.outbound.TransferRepository;
import com.aegis.payment.domain.port.outbound.WalletGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService - Application Service")
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private FraudAssessmentGateway fraudAssessmentGateway;

    @Mock
    private WalletGateway walletGateway;

    @Mock
    private TransactionTemplate transactionTemplate;

    private TransferService transferService;

    @BeforeEach
    void setUp() {
        // Make TransactionTemplate execute callbacks directly (no real transaction in unit tests).
        // lenient() because findById tests never invoke the transaction template.
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        transferService = new TransferService(
                transferRepository, eventPublisher, fraudAssessmentGateway,
                walletGateway, transactionTemplate);
    }

    @Nested
    @DisplayName("When executing transferFunds")
    class WhenExecutingTransferFunds {

        @Test
        @DisplayName("Should settle transfer to COMPLETED and publish TransferRequested + TransferCompleted when fraud APPROVE")
        void shouldPersistAndPublishOnFraudApprove() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    source, dest, user, new BigDecimal("50.00"), "EUR", "test", "ref-001");

            when(transferRepository.existsBySourceWalletIdAndReference(source, "ref-001")).thenReturn(false);
            when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenReturn(FraudDecision.APPROVE);
            when(walletGateway.createHold(eq(source), eq(new BigDecimal("50.00")), eq("EUR"), any()))
                    .thenReturn(UUID.randomUUID());
            when(walletGateway.settle(any(), any(), eq(source), eq(dest), eq(new BigDecimal("50.00")), eq("EUR")))
                    .thenReturn(new WalletGateway.SettlementResult(
                            new BigDecimal("150.00"), new BigDecimal("230.00")));

            Transfer result = transferService.execute(command);

            assertEquals(TransferStatus.COMPLETED, result.getStatus());
            assertEquals(source, result.getSourceWalletId());
            assertEquals(dest, result.getDestWalletId());
            assertNotNull(result.getHoldId());

            verify(walletGateway).createHold(eq(source), eq(new BigDecimal("50.00")), eq("EUR"), any());
            verify(walletGateway).settle(any(), any(), eq(source), eq(dest), eq(new BigDecimal("50.00")), eq("EUR"));
            verify(eventPublisher).publish(any(TransferRequested.class));
            verify(eventPublisher).publish(any(TransferCompleted.class));
            verify(eventPublisher, never()).publish(any(TransferFailed.class));
        }

        @Test
        @DisplayName("Should settle transfer to COMPLETED when fraud REVIEW")
        void shouldSettleOnFraudReview() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    source, dest, UUID.randomUUID(), new BigDecimal("50.00"), "EUR", null, "ref-001");

            when(transferRepository.existsBySourceWalletIdAndReference(source, "ref-001")).thenReturn(false);
            when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenReturn(FraudDecision.REVIEW);
            when(walletGateway.createHold(any(), any(), any(), any())).thenReturn(UUID.randomUUID());
            when(walletGateway.settle(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new WalletGateway.SettlementResult(
                            new BigDecimal("150.00"), new BigDecimal("230.00")));

            Transfer result = transferService.execute(command);

            assertEquals(TransferStatus.COMPLETED, result.getStatus());
            verify(fraudAssessmentGateway).assess(any());
            verify(walletGateway).settle(any(), any(), any(), any(), any(), any());
            verify(eventPublisher).publish(any(TransferCompleted.class));
            verify(eventPublisher, never()).publish(any(TransferFailed.class));
        }

        @Test
        @DisplayName("Should throw FraudRejectedException and publish TransferFailed when fraud REJECT")
        void shouldThrowFraudRejectedOnReject() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    source, dest, UUID.randomUUID(), new BigDecimal("50.00"), "EUR", null, "ref-001");

            when(transferRepository.existsBySourceWalletIdAndReference(source, "ref-001")).thenReturn(false);
            when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenReturn(FraudDecision.REJECT);

            FraudRejectedException ex = assertThrows(FraudRejectedException.class,
                    () -> transferService.execute(command));

            assertEquals("TRANSFER_REJECTED_BY_FRAUD", ex.getCode());
            verify(transferRepository, times(3)).save(any(Transfer.class));
            verify(eventPublisher).publish(any(TransferRequested.class));
            verify(eventPublisher).publish(any(TransferFailed.class));
        }

        @Test
        @DisplayName("Should throw FraudAssessmentUnavailableException and publish TransferFailed when gateway fails")
        void shouldThrowFraudUnavailableWhenGatewayFails() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    source, dest, UUID.randomUUID(), new BigDecimal("50.00"), "EUR", null, "ref-001");

            when(transferRepository.existsBySourceWalletIdAndReference(source, "ref-001")).thenReturn(false);
            when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenThrow(
                    new FraudAssessmentUnavailableException(new RuntimeException("timeout")));

            FraudAssessmentUnavailableException ex = assertThrows(FraudAssessmentUnavailableException.class,
                    () -> transferService.execute(command));

            assertEquals("FRAUD_UNAVAILABLE", ex.getCode());
            verify(transferRepository, times(3)).save(any(Transfer.class));
            verify(eventPublisher).publish(any(TransferRequested.class));
            verify(eventPublisher).publish(any(TransferFailed.class));
        }

        @Test
        @DisplayName("Should release hold and mark FAILED when settlement fails (saga compensation)")
        void shouldCompensateWhenSettlementFails() {
            UUID source = UUID.randomUUID();
            UUID dest = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    source, dest, UUID.randomUUID(), new BigDecimal("50.00"), "EUR", null, "ref-001");

            when(transferRepository.existsBySourceWalletIdAndReference(source, "ref-001")).thenReturn(false);
            when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenReturn(FraudDecision.APPROVE);
            when(walletGateway.createHold(any(), any(), any(), any())).thenReturn(holdId);
            when(walletGateway.settle(any(), any(), any(), any(), any(), any()))
                    .thenThrow(new SettlementFailedException("wallet down"));

            SettlementFailedException ex = assertThrows(SettlementFailedException.class,
                    () -> transferService.execute(command));

            assertEquals("SETTLEMENT_FAILED", ex.getCode());
            verify(walletGateway).release(source, holdId);
            verify(eventPublisher).publish(any(TransferRequested.class));
            verify(eventPublisher).publish(any(TransferFailed.class));
            verify(eventPublisher, never()).publish(any(TransferCompleted.class));
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

            verify(transferRepository, never()).save(any());
            verify(fraudAssessmentGateway, never()).assess(any());
        }

        @Test
        @DisplayName("Should throw SelfTransferException when wallets match")
        void shouldThrowSelfTransfer() {
            UUID wallet = UUID.randomUUID();
            TransferFundsUseCase.TransferCommand command = new TransferFundsUseCase.TransferCommand(
                    wallet, wallet, UUID.randomUUID(), new BigDecimal("50.00"), "EUR", null, "ref-001");

            assertThrows(SelfTransferException.class,
                    () -> transferService.execute(command));

            verify(transferRepository, never()).save(any());
            verify(fraudAssessmentGateway, never()).assess(any());
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
