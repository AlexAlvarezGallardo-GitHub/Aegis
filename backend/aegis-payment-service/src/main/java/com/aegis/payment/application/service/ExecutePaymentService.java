package com.aegis.payment.application.service;

import com.aegis.payment.domain.event.PaymentExecuted;
import com.aegis.payment.domain.event.PaymentFailed;
import com.aegis.payment.domain.event.PaymentRequested;
import com.aegis.payment.domain.exception.DuplicatePaymentException;
import com.aegis.payment.domain.exception.PaymentAssessmentUnavailableException;
import com.aegis.payment.domain.exception.PaymentNotFoundException;
import com.aegis.payment.domain.exception.PaymentRejectedException;
import com.aegis.payment.domain.exception.SettlementFailedException;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.port.inbound.ExecutePaymentUseCase;
import com.aegis.payment.domain.port.inbound.GetPaymentUseCase;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway.FraudDecision;
import com.aegis.payment.domain.port.outbound.FraudAssessmentGateway.TransactionContext;
import com.aegis.payment.domain.port.outbound.PaymentRepository;
import com.aegis.payment.domain.port.outbound.WalletGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Application service implementing the payment use cases.
 *
 * <p>The {@link #execute(ExecutePaymentUseCase.PaymentCommand)} method implements the
 * payment saga in two transactional phases:</p>
 * <ol>
 *   <li>Phase 1 — persist PENDING payment + publish PaymentRequested (committed)</li>
 *   <li>Phase 2 — synchronous fraud assessment; on REJECT or unavailability, persist
 *       FAILED + publish PaymentFailed, then throw the appropriate domain exception.
 *       On APPROVE/REVIEW: create hold → debit wallet → complete → publish PaymentExecuted.
 *       On settlement failure: compensate (release hold) → fail → publish PaymentFailed.</li>
 * </ol>
 */
@Service
public class ExecutePaymentService implements ExecutePaymentUseCase, GetPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExecutePaymentService.class);

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final FraudAssessmentGateway fraudAssessmentGateway;
    private final WalletGateway walletGateway;
    private final TransactionTemplate transactionTemplate;

    public ExecutePaymentService(PaymentRepository paymentRepository,
                                 EventPublisher eventPublisher,
                                 FraudAssessmentGateway fraudAssessmentGateway,
                                 WalletGateway walletGateway,
                                 TransactionTemplate transactionTemplate) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.fraudAssessmentGateway = fraudAssessmentGateway;
        this.walletGateway = walletGateway;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Payment execute(PaymentCommand command) {
        // Phase 1 — persist PENDING payment + publish PaymentRequested (own transaction)
        Payment payment = transactionTemplate.execute(status ->
                persistPendingPayment(command));

        // Phase 2 — fraud assessment + hold + debit + complete
        return executeSaga(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment findById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private Payment persistPendingPayment(PaymentCommand command) {
        validate(command);

        if (paymentRepository.existsByWalletIdAndReference(command.walletId(), command.reference())) {
            throw new DuplicatePaymentException(command.reference());
        }

        Payment payment = Payment.request(
                command.walletId(),
                command.userId(),
                command.amount(),
                command.currency(),
                command.payee(),
                command.description(),
                command.reference()
        );

        Payment saved = paymentRepository.save(payment);
        eventPublisher.publish(new PaymentRequested(saved));
        log.info("Payment requested: id={}, reference={}", saved.getId(), saved.getReference());
        return saved;
    }

    private Payment executeSaga(Payment payment) {
        AtomicReference<RuntimeException> postCommitException = new AtomicReference<>();

        Payment result = transactionTemplate.execute(status -> {
            payment.startProcessing();
            paymentRepository.save(payment);

            // Fraud assessment
            FraudDecision decision;
            try {
                TransactionContext ctx = new TransactionContext(
                        payment.getId(),
                        "PAYMENT",
                        payment.getWalletId(),
                        null,
                        payment.getUserId(),
                        payment.getAmount(),
                        payment.getCurrency()
                );
                decision = fraudAssessmentGateway.assess(ctx);
            } catch (PaymentAssessmentUnavailableException ex) {
                payment.fail("FRAUD_UNAVAILABLE");
                paymentRepository.save(payment);
                eventPublisher.publish(new PaymentFailed(payment, false));
                log.warn("Fraud assessment unavailable for payment {}: {}",
                        payment.getId(), ex.getMessage());
                postCommitException.set(ex);
                return payment;
            }

            if (decision == FraudDecision.REJECT) {
                payment.fail("FRAUD_REJECTED");
                paymentRepository.save(payment);
                eventPublisher.publish(new PaymentFailed(payment, false));
                log.info("Payment rejected by fraud assessment: id={}", payment.getId());
                postCommitException.set(new PaymentRejectedException(payment.getId()));
                return payment;
            }

            // APPROVE or REVIEW — create hold
            UUID holdId;
            try {
                holdId = walletGateway.createHold(
                        payment.getWalletId(),
                        payment.getAmount(),
                        payment.getCurrency(),
                        payment.getId().toString());
            } catch (SettlementFailedException ex) {
                payment.fail(ex.getCode());
                paymentRepository.save(payment);
                eventPublisher.publish(new PaymentFailed(payment, false));
                log.warn("Hold creation failed for payment {}: {}",
                        payment.getId(), ex.getMessage());
                postCommitException.set(ex);
                return payment;
            }

            payment.markFundsReserved(holdId);
            paymentRepository.save(payment);

            // Debit wallet
            try {
                walletGateway.debitHold(
                        payment.getId(),
                        holdId,
                        payment.getWalletId(),
                        payment.getAmount(),
                        payment.getCurrency());
                payment.complete();
                paymentRepository.save(payment);
                eventPublisher.publish(new PaymentExecuted(payment));
                log.info("Payment completed: id={}", payment.getId());
                return payment;
            } catch (SettlementFailedException ex) {
                compensate(payment, ex);
                postCommitException.set(ex);
                return payment;
            }
        });

        if (postCommitException.get() != null) {
            throw postCommitException.get();
        }
        return result;
    }

    private void compensate(Payment payment, SettlementFailedException cause) {
        try {
            if (payment.getHoldId() != null) {
                walletGateway.release(payment.getWalletId(), payment.getHoldId());
                log.info("Released hold {} as compensation for failed payment {}",
                        payment.getHoldId(), payment.getId());
            }
        } catch (SettlementFailedException releaseEx) {
            log.error("Compensation failed — hold {} could not be released for payment {}",
                    payment.getHoldId(), payment.getId(), releaseEx);
        }
        payment.fail(cause.getCode());
        paymentRepository.save(payment);
        eventPublisher.publish(new PaymentFailed(payment, true));
    }

    private void validate(PaymentCommand command) {
        if (command.walletId() == null) {
            throw new IllegalArgumentException("walletId is required");
        }
        if (command.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (command.currency() == null || !command.currency().matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("currency must be a 3-letter uppercase ISO code");
        }
        if (command.payee() == null) {
            throw new IllegalArgumentException("payee is required");
        }
        if (command.reference() == null || command.reference().isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
    }
}
