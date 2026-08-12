package com.aegis.payment.application.service;

import com.aegis.payment.domain.event.PaymentExecuted;
import com.aegis.payment.domain.event.PaymentFailed;
import com.aegis.payment.domain.event.PaymentRequested;
import com.aegis.payment.domain.exception.DuplicatePaymentException;
import com.aegis.payment.domain.exception.PaymentAssessmentUnavailableException;
import com.aegis.payment.domain.exception.PaymentNotFoundException;
import com.aegis.payment.domain.exception.PaymentRejectedException;
import com.aegis.payment.domain.exception.SettlementFailedException;
import com.aegis.payment.domain.model.Payee;
import com.aegis.payment.domain.model.PayeeType;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.model.PaymentStatus;
import com.aegis.payment.domain.port.inbound.ExecutePaymentUseCase;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway.FraudDecision;
import com.aegis.payment.domain.port.outbound.PaymentRepository;
import com.aegis.payment.domain.port.outbound.WalletGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@DisplayName("ExecutePaymentService - Application Service")
class ExecutePaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private FraudAssessmentGateway fraudAssessmentGateway;
    @Mock private WalletGateway walletGateway;
    @Mock private TransactionTemplate transactionTemplate;

    private ExecutePaymentService service;
    private static final Payee PAYEE = new Payee("Cafe Central", "merchant-123", PayeeType.MERCHANT);

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        service = new ExecutePaymentService(
                paymentRepository, eventPublisher, fraudAssessmentGateway,
                walletGateway, transactionTemplate);
    }

    @Nested
    @DisplayName("When executing a payment")
    class WhenExecutingPayment {

        @Test
        @DisplayName("Should complete payment and publish events when fraud APPROVE")
        void shouldCompleteOnFraudApprove() {
            UUID wallet = UUID.randomUUID();
            UUID user = UUID.randomUUID();
            ExecutePaymentUseCase.PaymentCommand command = new ExecutePaymentUseCase.PaymentCommand(
                    wallet, user, new BigDecimal("25.00"), "EUR", PAYEE, "Coffee", "PAY-001");

            when(paymentRepository.existsByWalletIdAndReference(wallet, "PAY-001")).thenReturn(false);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenReturn(FraudDecision.APPROVE);
            when(walletGateway.createHold(eq(wallet), eq(new BigDecimal("25.00")), eq("EUR"), any()))
                    .thenReturn(UUID.randomUUID());
            when(walletGateway.debitHold(any(), any(), eq(wallet), eq(new BigDecimal("25.00")), eq("EUR")))
                    .thenReturn(new BigDecimal("175.00"));

            Payment result = service.execute(command);

            assertEquals(PaymentStatus.COMPLETED, result.getStatus());
            assertEquals(wallet, result.getWalletId());
            assertNotNull(result.getHoldId());

            verify(walletGateway).createHold(eq(wallet), eq(new BigDecimal("25.00")), eq("EUR"), any());
            verify(walletGateway).debitHold(any(), any(), eq(wallet), eq(new BigDecimal("25.00")), eq("EUR"));
            verify(eventPublisher).publish(any(PaymentRequested.class));
            verify(eventPublisher).publish(any(PaymentExecuted.class));
            verify(eventPublisher, never()).publish(any(PaymentFailed.class));
        }

        @Test
        @DisplayName("Should complete payment when fraud REVIEW")
        void shouldCompleteOnFraudReview() {
            ExecutePaymentUseCase.PaymentCommand command = new ExecutePaymentUseCase.PaymentCommand(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("25.00"), "EUR",
                    PAYEE, null, "PAY-001");

            when(paymentRepository.existsByWalletIdAndReference(any(), any())).thenReturn(false);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenReturn(FraudDecision.REVIEW);
            when(walletGateway.createHold(any(), any(), any(), any())).thenReturn(UUID.randomUUID());
            when(walletGateway.debitHold(any(), any(), any(), any(), any()))
                    .thenReturn(new BigDecimal("175.00"));

            Payment result = service.execute(command);

            assertEquals(PaymentStatus.COMPLETED, result.getStatus());
            verify(eventPublisher).publish(any(PaymentExecuted.class));
        }

        @Test
        @DisplayName("Should throw PaymentRejectedException when fraud REJECT")
        void shouldThrowOnFraudReject() {
            ExecutePaymentUseCase.PaymentCommand command = new ExecutePaymentUseCase.PaymentCommand(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("25.00"), "EUR",
                    PAYEE, null, "PAY-001");

            when(paymentRepository.existsByWalletIdAndReference(any(), any())).thenReturn(false);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenReturn(FraudDecision.REJECT);

            PaymentRejectedException ex = assertThrows(PaymentRejectedException.class,
                    () -> service.execute(command));

            assertEquals("PAYMENT_REJECTED_BY_FRAUD", ex.getCode());
            verify(eventPublisher).publish(any(PaymentRequested.class));
            verify(eventPublisher).publish(any(PaymentFailed.class));
        }

        @Test
        @DisplayName("Should throw PaymentAssessmentUnavailableException when fraud unavailable")
        void shouldThrowOnFraudUnavailable() {
            ExecutePaymentUseCase.PaymentCommand command = new ExecutePaymentUseCase.PaymentCommand(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("25.00"), "EUR",
                    PAYEE, null, "PAY-001");

            when(paymentRepository.existsByWalletIdAndReference(any(), any())).thenReturn(false);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenThrow(
                    new PaymentAssessmentUnavailableException("timeout", new RuntimeException()));

            assertThrows(PaymentAssessmentUnavailableException.class,
                    () -> service.execute(command));

            verify(eventPublisher).publish(any(PaymentFailed.class));
        }

        @Test
        @DisplayName("Should compensate and fail when settlement fails")
        void shouldCompensateOnSettlementFailure() {
            UUID wallet = UUID.randomUUID();
            UUID holdId = UUID.randomUUID();
            ExecutePaymentUseCase.PaymentCommand command = new ExecutePaymentUseCase.PaymentCommand(
                    wallet, UUID.randomUUID(), new BigDecimal("25.00"), "EUR",
                    PAYEE, null, "PAY-001");

            when(paymentRepository.existsByWalletIdAndReference(any(), any())).thenReturn(false);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(fraudAssessmentGateway.assess(any())).thenReturn(FraudDecision.APPROVE);
            when(walletGateway.createHold(any(), any(), any(), any())).thenReturn(holdId);
            when(walletGateway.debitHold(any(), any(), any(), any(), any()))
                    .thenThrow(new SettlementFailedException("wallet down"));

            assertThrows(SettlementFailedException.class,
                    () -> service.execute(command));

            verify(walletGateway).release(wallet, holdId);
            verify(eventPublisher).publish(any(PaymentFailed.class));
            verify(eventPublisher, never()).publish(any(PaymentExecuted.class));
        }

        @Test
        @DisplayName("Should throw DuplicatePaymentException when reference exists")
        void shouldThrowOnDuplicate() {
            UUID wallet = UUID.randomUUID();
            ExecutePaymentUseCase.PaymentCommand command = new ExecutePaymentUseCase.PaymentCommand(
                    wallet, UUID.randomUUID(), new BigDecimal("25.00"), "EUR",
                    PAYEE, null, "PAY-DUP");

            when(paymentRepository.existsByWalletIdAndReference(wallet, "PAY-DUP")).thenReturn(true);

            assertThrows(DuplicatePaymentException.class,
                    () -> service.execute(command));

            verify(paymentRepository, never()).save(any());
            verify(fraudAssessmentGateway, never()).assess(any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when amount is zero")
        void shouldThrowOnZeroAmount() {
            ExecutePaymentUseCase.PaymentCommand command = new ExecutePaymentUseCase.PaymentCommand(
                    UUID.randomUUID(), UUID.randomUUID(), BigDecimal.ZERO, "EUR",
                    PAYEE, null, "PAY-001");

            assertThrows(IllegalArgumentException.class,
                    () -> service.execute(command));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when payee is null")
        void shouldThrowOnNullPayee() {
            ExecutePaymentUseCase.PaymentCommand command = new ExecutePaymentUseCase.PaymentCommand(
                    UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("25.00"), "EUR",
                    null, null, "PAY-001");

            assertThrows(IllegalArgumentException.class,
                    () -> service.execute(command));
        }
    }

    @Nested
    @DisplayName("When finding a payment by ID")
    class WhenFindingPayment {

        @Test
        @DisplayName("Should return payment when found")
        void shouldReturnPayment() {
            UUID id = UUID.randomUUID();
            Payment payment = Payment.request(UUID.randomUUID(), UUID.randomUUID(),
                    new BigDecimal("25.00"), "EUR", PAYEE, null, "PAY-001");
            Payment rehydrated = Payment.rehydrate(id, payment.getWalletId(),
                    payment.getUserId(), payment.getAmount(), payment.getCurrency(),
                    payment.getPayee(), payment.getDescription(), payment.getReference(),
                    payment.getStatus(), null, null, null,
                    payment.getCreatedAt(), payment.getUpdatedAt(), null);

            when(paymentRepository.findById(id)).thenReturn(Optional.of(rehydrated));

            Payment result = service.findById(id);
            assertEquals(id, result.getId());
        }

        @Test
        @DisplayName("Should throw PaymentNotFoundException when not found")
        void shouldThrowNotFound() {
            UUID id = UUID.randomUUID();
            when(paymentRepository.findById(id)).thenReturn(Optional.empty());

            assertThrows(PaymentNotFoundException.class,
                    () -> service.findById(id));
        }
    }
}
