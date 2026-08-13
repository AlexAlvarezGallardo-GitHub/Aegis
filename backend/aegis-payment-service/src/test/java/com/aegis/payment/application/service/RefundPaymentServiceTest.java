package com.aegis.payment.application.service;

import com.aegis.payment.application.dto.RefundResult;
import com.aegis.payment.domain.event.PaymentRefunded;
import com.aegis.payment.domain.exception.PaymentAlreadyRefundedException;
import com.aegis.payment.domain.exception.PaymentNotFoundException;
import com.aegis.payment.domain.exception.PaymentNotOwnedException;
import com.aegis.payment.domain.exception.PaymentNotRefundableException;
import com.aegis.payment.domain.exception.RefundExceedsPaymentException;
import com.aegis.payment.domain.exception.SettlementFailedException;
import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.PayeeType;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.model.PaymentStatus;
import com.aegis.payment.domain.model.Refund;
import com.aegis.payment.domain.model.RefundStatus;
import com.aegis.payment.domain.port.inbound.RefundPaymentUseCase;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.domain.port.outbound.PaymentRepository;
import com.aegis.payment.domain.port.outbound.RefundRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefundPaymentService - Refund Saga")
class RefundPaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private WalletGateway walletGateway;
    @Mock private TransactionTemplate transactionTemplate;

    private RefundPaymentService service;

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID WALLET_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Payee PAYEE = new Payee("Cafe Central", "merchant-123", PayeeType.MERCHANT);

    @BeforeEach
    void setUp() {
        service = new RefundPaymentService(paymentRepository, refundRepository, eventPublisher,
                walletGateway, transactionTemplate);
    }

    private Payment completedPayment() {
        return Payment.rehydrate(PAYMENT_ID, WALLET_ID, USER_ID,
                new BigDecimal("25.00"), "EUR", PAYEE, "Coffee", "PAY-001",
                PaymentStatus.COMPLETED, UUID.randomUUID(), UUID.randomUUID(), null,
                java.time.Instant.now(), java.time.Instant.now(), java.time.Instant.now());
    }

    private RefundPaymentUseCase.RefundCommand command(BigDecimal amount, String reference) {
        return new RefundPaymentUseCase.RefundCommand(PAYMENT_ID, USER_ID, amount, null, reference, false);
    }

    @Nested
    @DisplayName("When refunding a completed payment")
    class WhenRefundingCompletedPayment {

        @Test
        @DisplayName("Should complete full refund successfully")
        void shouldCompleteFullRefund() {
            Payment payment = completedPayment();
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(refundRepository.existsByReference("REF-001")).thenReturn(false);
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(walletGateway.creditRefund(any(), any(), any(), any())).thenReturn(new BigDecimal("100.00"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            RefundResult result = service.refund(command(null, "REF-001"));

            assertEquals(RefundStatus.COMPLETED, result.status());
            assertEquals(PAYMENT_ID, result.paymentId());
            verify(walletGateway).creditRefund(any(), eq(WALLET_ID), eq(new BigDecimal("25.00")), eq("EUR"));
            verify(eventPublisher).publish(any(PaymentRefunded.class));
        }

        @Test
        @DisplayName("Should complete partial refund successfully")
        void shouldCompletePartialRefund() {
            Payment payment = completedPayment();
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(refundRepository.existsByReference("REF-002")).thenReturn(false);
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(walletGateway.creditRefund(any(), any(), any(), any())).thenReturn(new BigDecimal("85.00"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            RefundResult result = service.refund(command(new BigDecimal("10.00"), "REF-002"));

            assertEquals(RefundStatus.COMPLETED, result.status());
            assertEquals(new BigDecimal("10.00"), result.amount());
            verify(walletGateway).creditRefund(any(), eq(WALLET_ID), eq(new BigDecimal("10.00")), eq("EUR"));
        }

        @Test
        @DisplayName("Should return existing refund for duplicate reference (idempotent)")
        void shouldBeIdempotentForDuplicateReference() {
            Refund existing = Refund.rehydrate(UUID.randomUUID(), PAYMENT_ID, WALLET_ID, USER_ID,
                    new BigDecimal("25.00"), "EUR", null, "REF-DUP",
                    RefundStatus.COMPLETED, java.time.Instant.now(), java.time.Instant.now(), java.time.Instant.now());
            when(refundRepository.findByReference("REF-DUP")).thenReturn(Optional.of(existing));

            RefundResult result = service.refund(command(null, "REF-DUP"));

            assertEquals(existing.getId(), result.refundId());
            verifyNoInteractions(paymentRepository);
            verifyNoInteractions(walletGateway);
        }
    }

    @Nested
    @DisplayName("When refund validation fails")
    class WhenRefundValidationFails {

        @Test
        @DisplayName("Should throw PaymentNotFoundException when payment does not exist")
        void shouldThrowWhenPaymentNotFound() {
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class,
                    () -> service.refund(command(null, "REF-001")));
        }

        @Test
        @DisplayName("Should throw PaymentAlreadyRefundedException when payment is REFUNDED")
        void shouldThrowWhenPaymentAlreadyRefunded() {
            Payment payment = Payment.rehydrate(PAYMENT_ID, WALLET_ID, USER_ID,
                    new BigDecimal("25.00"), "EUR", PAYEE, null, "PAY-001",
                    PaymentStatus.REFUNDED, UUID.randomUUID(), UUID.randomUUID(), null,
                    java.time.Instant.now(), java.time.Instant.now(), java.time.Instant.now());
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));

            assertThrows(PaymentAlreadyRefundedException.class,
                    () -> service.refund(command(null, "REF-001")));
        }

        @Test
        @DisplayName("Should throw PaymentNotRefundableException when payment is FAILED")
        void shouldThrowWhenPaymentNotRefundable() {
            Payment payment = Payment.rehydrate(PAYMENT_ID, WALLET_ID, USER_ID,
                    new BigDecimal("25.00"), "EUR", PAYEE, null, "PAY-001",
                    PaymentStatus.FAILED, UUID.randomUUID(), UUID.randomUUID(), "FRAUD",
                    java.time.Instant.now(), java.time.Instant.now(), null);
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));

            assertThrows(PaymentNotRefundableException.class,
                    () -> service.refund(command(null, "REF-001")));
        }

        @Test
        @DisplayName("Should throw PaymentNotOwnedException when user does not own payment")
        void shouldThrowWhenUserDoesNotOwnPayment() {
            Payment payment = completedPayment();
            UUID otherUser = UUID.randomUUID();
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));

            RefundPaymentUseCase.RefundCommand cmd = new RefundPaymentUseCase.RefundCommand(
                    PAYMENT_ID, otherUser, null, null, "REF-001", false);
            assertThrows(PaymentNotOwnedException.class, () -> service.refund(cmd));
        }

        @Test
        @DisplayName("Should allow admin override for non-owned payment")
        void shouldAllowAdminOverride() {
            Payment payment = completedPayment();
            UUID otherUser = UUID.randomUUID();
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(refundRepository.existsByReference("REF-001")).thenReturn(false);
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(walletGateway.creditRefund(any(), any(), any(), any())).thenReturn(new BigDecimal("100.00"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            RefundPaymentUseCase.RefundCommand cmd = new RefundPaymentUseCase.RefundCommand(
                    PAYMENT_ID, otherUser, null, null, "REF-001", true);
            RefundResult result = service.refund(cmd);

            assertEquals(RefundStatus.COMPLETED, result.status());
        }

        @Test
        @DisplayName("Should throw RefundExceedsPaymentException when amount > payment amount")
        void shouldThrowWhenAmountExceedsPayment() {
            Payment payment = completedPayment();
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));

            assertThrows(RefundExceedsPaymentException.class,
                    () -> service.refund(command(new BigDecimal("50.00"), "REF-001")));
        }
    }

    @Nested
    @DisplayName("When wallet credit fails")
    class WhenWalletCreditFails {

        @Test
        @DisplayName("Should fail refund and rethrow exception")
        void shouldFailRefundAndRethrow() {
            Payment payment = completedPayment();
            when(transactionTemplate.execute(any())).thenAnswer(inv -> {
                TransactionCallback<?> cb = inv.getArgument(0);
                return cb.doInTransaction(null);
            });
            when(paymentRepository.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
            when(refundRepository.existsByReference("REF-001")).thenReturn(false);
            when(refundRepository.save(any(Refund.class))).thenAnswer(inv -> inv.getArgument(0));
            when(walletGateway.creditRefund(any(), any(), any(), any()))
                    .thenThrow(new SettlementFailedException("Wallet unreachable"));

            assertThrows(SettlementFailedException.class,
                    () -> service.refund(command(null, "REF-001")));

            // Verify refund was saved at least once with FAILED status
            ArgumentCaptor<Refund> captor = ArgumentCaptor.forClass(Refund.class);
            verify(refundRepository, atLeastOnce()).save(captor.capture());
            // The last save should be the FAILED one
            boolean hasFailed = captor.getAllValues().stream()
                    .anyMatch(r -> r.getStatus() == RefundStatus.FAILED);
            assertTrue(hasFailed, "Expected at least one save with FAILED status");
        }
    }
}
