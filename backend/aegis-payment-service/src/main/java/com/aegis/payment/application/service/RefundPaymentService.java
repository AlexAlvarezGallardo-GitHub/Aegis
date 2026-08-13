package com.aegis.payment.application.service;

import com.aegis.payment.domain.event.PaymentRefunded;
import com.aegis.payment.domain.exception.PaymentAlreadyRefundedException;
import com.aegis.payment.domain.exception.PaymentNotFoundException;
import com.aegis.payment.domain.exception.PaymentNotOwnedException;
import com.aegis.payment.domain.exception.PaymentNotRefundableException;
import com.aegis.payment.domain.exception.RefundAlreadyExistsException;
import com.aegis.payment.domain.exception.RefundExceedsPaymentException;
import com.aegis.payment.domain.exception.SettlementFailedException;
import com.aegis.payment.domain.model.Payment;
import com.aegis.payment.domain.model.PaymentStatus;
import com.aegis.payment.domain.model.Refund;
import com.aegis.payment.domain.port.inbound.GetRefundUseCase;
import com.aegis.payment.domain.port.inbound.RefundPaymentUseCase;
import com.aegis.payment.domain.port.outbound.EventPublisher;
import com.aegis.payment.domain.port.outbound.PaymentRepository;
import com.aegis.payment.domain.port.outbound.RefundRepository;
import com.aegis.payment.domain.port.outbound.WalletGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Application service implementing the refund use case.
 *
 * <p>The {@link #refund(RefundPaymentUseCase.RefundCommand)} method implements the
 * refund saga in two transactional phases:</p>
 * <ol>
 *   <li>Phase 1 — validate payment, persist PENDING refund (committed)</li>
 *   <li>Phase 2 — credit wallet → complete refund → mark payment REFUNDED →
 *       publish PaymentRefunded. On wallet failure: fail refund → rethrow.</li>
 * </ol>
 */
@Service
public class RefundPaymentService implements RefundPaymentUseCase, GetRefundUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefundPaymentService.class);

    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final EventPublisher eventPublisher;
    private final WalletGateway walletGateway;
    private final TransactionTemplate transactionTemplate;

    public RefundPaymentService(PaymentRepository paymentRepository,
                                RefundRepository refundRepository,
                                EventPublisher eventPublisher,
                                WalletGateway walletGateway,
                                TransactionTemplate transactionTemplate) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.eventPublisher = eventPublisher;
        this.walletGateway = walletGateway;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Refund refund(RefundCommand command) {
        validate(command);

        // Idempotency: if a refund with the same reference already exists, return it
        var existingRefund = refundRepository.findByReference(command.reference());
        if (existingRefund.isPresent()) {
            log.info("Refund with reference {} already exists, returning existing", command.reference());
            return existingRefund.get();
        }

        // Phase 1 — validate payment + persist PENDING refund (own transaction)
        Refund refund = transactionTemplate.execute(status ->
                persistPendingRefund(command));

        // Phase 2 — credit wallet + complete refund + mark payment REFUNDED
        return completeRefundSaga(refund);
    }

    @Override
    public Refund findById(UUID refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new com.aegis.payment.domain.exception.RefundNotFoundException(refundId));
    }

    private Refund persistPendingRefund(RefundCommand command) {
        // Lock payment pessimistically to prevent concurrent refunds
        Payment payment = paymentRepository.findByIdForUpdate(command.paymentId())
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));

        // Validate payment is COMPLETED
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentAlreadyRefundedException(payment.getId());
        }
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentNotRefundableException(payment.getId());
        }

        // Validate ownership (unless admin override)
        if (!command.adminOverride() && !payment.getUserId().equals(command.userId())) {
            throw new PaymentNotOwnedException(payment.getId(), command.userId());
        }

        // Determine refund amount (full if not specified)
        BigDecimal refundAmount = command.amount() != null ? command.amount() : payment.getAmount();

        // Validate refund amount ≤ payment amount
        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new RefundExceedsPaymentException(payment.getId());
        }

        // Check for duplicate reference
        if (refundRepository.existsByReference(command.reference())) {
            throw new RefundAlreadyExistsException(command.reference());
        }

        Refund refund = Refund.request(
                payment.getId(),
                payment.getWalletId(),
                payment.getUserId(),
                refundAmount,
                payment.getCurrency(),
                command.reason(),
                command.reference()
        );

        Refund saved = refundRepository.save(refund);
        log.info("Refund requested: id={}, paymentId={}, reference={}",
                saved.getId(), saved.getPaymentId(), saved.getReference());
        return saved;
    }

    private Refund completeRefundSaga(Refund refund) {
        AtomicReference<RuntimeException> postCommitException = new AtomicReference<>();
        AtomicReference<BigDecimal> newBalanceRef = new AtomicReference<>();

        Refund result = transactionTemplate.execute(status -> {
            // Credit wallet
            BigDecimal newBalance;
            try {
                newBalance = walletGateway.creditRefund(
                        refund.getId(),
                        refund.getWalletId(),
                        refund.getAmount(),
                        refund.getCurrency()
                );
            } catch (SettlementFailedException ex) {
                refund.fail("WALLET_CREDIT_FAILED");
                refundRepository.save(refund);
                log.warn("Wallet credit failed for refund {}: {}",
                        refund.getId(), ex.getMessage());
                postCommitException.set(ex);
                return refund;
            }

            newBalanceRef.set(newBalance);
            refund.complete();
            refundRepository.save(refund);

            // Mark payment as REFUNDED
            Payment payment = paymentRepository.findByIdForUpdate(refund.getPaymentId())
                    .orElseThrow(() -> new PaymentNotFoundException(refund.getPaymentId()));
            payment.markRefunded();
            paymentRepository.save(payment);

            // Publish event
            eventPublisher.publish(new PaymentRefunded(refund, newBalance));
            log.info("Refund completed: id={}, paymentId={}, newBalance={}",
                    refund.getId(), refund.getPaymentId(), newBalance);
            return refund;
        });

        if (postCommitException.get() != null) {
            throw postCommitException.get();
        }
        return result;
    }

    private void validate(RefundCommand command) {
        if (command.paymentId() == null) {
            throw new IllegalArgumentException("paymentId is required");
        }
        if (command.userId() == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (command.reference() == null || command.reference().isBlank()) {
            throw new IllegalArgumentException("reference is required");
        }
        if (command.amount() != null && command.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive if provided");
        }
    }
}
